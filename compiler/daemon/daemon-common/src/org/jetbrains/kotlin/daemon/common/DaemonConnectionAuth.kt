/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.AclEntry
import java.nio.file.attribute.AclEntryFlag
import java.nio.file.attribute.AclEntryPermission
import java.nio.file.attribute.AclEntryType
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap

const val DAEMON_AUTH_SECRET_SIZE: Int = 32
const val DAEMON_AUTH_HANDSHAKE_TIMEOUT_MS: Int = 5000

/**
 * Authentication of the loopback RMI connections between the compile daemon and its clients.
 *
 * The handshake is symmetric:
 *  - a client connecting to a daemon sends the secret it has read from that daemon's run file
 *    (registered per port via [registerDaemonSecret]);
 *  - a daemon calling back into the services exported by a client sends its own secret ([setOwnSecret]),
 *    which the client accepts because it has read it from the run file.
 */
object DaemonConnectionAuth {
    // 4 characters to match the magic prefix length of RMI ("JRMI")
    private val MAGIC: ByteArray = "KCD1".toByteArray(Charsets.US_ASCII)

    @Volatile
    private var ownSecret: ByteArray? = null
    private val daemonSecretsByPort = ConcurrentHashMap<Int, ByteArray>()

    fun generateSecret(): ByteArray = ByteArray(DAEMON_AUTH_SECRET_SIZE).also { SecureRandom().nextBytes(it) }

    /**
     * Sets the secret this process publishes to its clients. Called by the daemon once on startup, before it discovers
     * any other daemons: a process that already acts as a client of other daemons cannot become a daemon itself.
     */
    fun setOwnSecret(secret: ByteArray) {
        require(secret.size == DAEMON_AUTH_SECRET_SIZE) { "Unexpected secret size: ${secret.size}" }
        synchronized(this) {
            check(ownSecret == null) { "The daemon secret is already set" }
            check(daemonSecretsByPort.isEmpty()) { "Cannot become a daemon after connecting to other daemons as a client" }
            ownSecret = secret.copyOf()
        }
    }

    /**
     * Registers the secret of the daemon listening on [port], as read from its run file.
     * Called by every process that discovers daemons: the clients, and the daemons themselves during elections.
     */
    fun registerDaemonSecret(port: Int, secret: ByteArray) {
        require(secret.size == DAEMON_AUTH_SECRET_SIZE) { "Unexpected secret size: ${secret.size}" }
        daemonSecretsByPort[port] = secret.copyOf()
    }

    /**
     * The secret to present when connecting to [port]: the secret of the daemon discovered on that port, if any.
     * Otherwise, for a daemon, its own secret: that is how it authenticates the callbacks into the services exported by its
     * clients on ports it has no run file for, and the clients accept it because they have read it from the run file.
     * A client never falls back and cannot connect to a port it has not discovered a daemon on.
     */
    internal fun secretForOutgoingConnection(port: Int): ByteArray? = daemonSecretsByPort[port] ?: ownSecret

    private fun isAccepted(secret: ByteArray): Boolean {
        ownSecret?.let { return MessageDigest.isEqual(it, secret) }
        return daemonSecretsByPort.values.any { MessageDigest.isEqual(it, secret) }
    }

    internal fun writeHandshake(out: OutputStream, secret: ByteArray) {
        out.write(MAGIC)
        out.write(secret)
        out.flush()
    }

    internal fun readHandshake(input: InputStream): Boolean {
        val magic = ByteArray(MAGIC.size)
        if (!input.readFully(magic) || !magic.contentEquals(MAGIC)) return false
        val secret = ByteArray(DAEMON_AUTH_SECRET_SIZE)
        return input.readFully(secret) && isAccepted(secret)
    }

    private fun InputStream.readFully(buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read < 0) return false
            offset += read
        }
        return true
    }
}

class AuthenticatingClientSocket(address: InetAddress, port: Int) : Socket(address, port) {
    init {
        try {
            val secret = DaemonConnectionAuth.secretForOutgoingConnection(port)
                ?: throw IOException("No authentication secret is known for the Kotlin compile daemon on port $port")
            DaemonConnectionAuth.writeHandshake(getOutputStream(), secret)
        } catch (e: IOException) {
            runCatching { close() }
            throw e
        }
    }
}

class AuthenticatingServerSocket(port: Int, backlog: Int, bindAddress: InetAddress) : ServerSocket(port, backlog, bindAddress) {
    override fun accept(): Socket {
        if (isClosed) throw SocketException("Socket is closed")
        if (!isBound) throw SocketException("Socket is not bound yet")
        return AuthenticatingSocket().also { implAccept(it) }
    }
}

private class AuthenticatingSocket : Socket() {
    private val lock = Any()

    @Volatile
    private var authenticated = false
    private var inputStream: InputStream? = null

    override fun getInputStream(): InputStream = synchronized(lock) {
        inputStream ?: AuthenticatingInputStream(super.getInputStream()).also { inputStream = it }
    }

    private fun ensureAuthenticated(rawInput: InputStream) {
        if (authenticated) return
        synchronized(lock) {
            if (authenticated) return
            val accepted = try {
                val previousTimeout = soTimeout
                soTimeout = DAEMON_AUTH_HANDSHAKE_TIMEOUT_MS
                try {
                    DaemonConnectionAuth.readHandshake(rawInput)
                } finally {
                    soTimeout = previousTimeout
                }
            } catch (_: IOException) {
                false
            }
            if (!accepted) {
                runCatching { close() }
                throw IOException("The connection to the Kotlin compile daemon is not authenticated, closing it")
            }
            authenticated = true
        }
    }

    private inner class AuthenticatingInputStream(rawInput: InputStream) : FilterInputStream(rawInput) {
        override fun read(): Int {
            ensureAuthenticated(`in`)
            return super.read()
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            ensureAuthenticated(`in`)
            return super.read(b, off, len)
        }

        override fun skip(n: Long): Long {
            ensureAuthenticated(`in`)
            return super.skip(n)
        }

        override fun available(): Int = if (authenticated) super.available() else 0
    }
}

fun createRunFileWithSecret(runFile: File, secret: ByteArray) {
    val directory = runFile.absoluteFile.parentFile.toPath()
    if (runFile.exists()) throw IOException("The run file already exists")
    val tempFile = Files.createTempFile(directory, runFile.name, ".tmp")
    try {
        restrictAccessToOwner(tempFile)
        Files.write(tempFile, secret.toHexString().toByteArray(Charsets.US_ASCII))
        Files.move(tempFile, runFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
    } finally {
        Files.deleteIfExists(tempFile)
    }
}

fun readRunFileSecret(runFile: File): ByteArray? =
    try {
        runFile.readText(Charsets.US_ASCII).trim().hexToByteArray().takeIf { it.size == DAEMON_AUTH_SECRET_SIZE }
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

private fun restrictAccessToOwner(path: Path) {
    try {
        val posixView = Files.getFileAttributeView(path, PosixFileAttributeView::class.java)
        if (posixView != null) {
            posixView.setPermissions(PosixFilePermissions.fromString("rw-------"))
            return
        }
        val aclView = Files.getFileAttributeView(path, AclFileAttributeView::class.java) ?: return
        val ownerOnly = AclEntry.newBuilder()
            .setType(AclEntryType.ALLOW)
            .setPrincipal(aclView.owner)
            .setPermissions(EnumSet.allOf(AclEntryPermission::class.java))
            .setFlags(EnumSet.noneOf(AclEntryFlag::class.java))
            .build()
        aclView.acl = listOf(ownerOnly)
    } catch (_: Exception) {
        // best effort: the default run files directory is located in the user-private area anyway
    }
}
