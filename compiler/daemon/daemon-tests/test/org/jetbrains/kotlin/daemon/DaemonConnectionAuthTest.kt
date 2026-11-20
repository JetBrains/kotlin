/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.COMPILER_SERVICE_RMI_NAME
import org.jetbrains.kotlin.daemon.common.DAEMON_AUTH_SECRET_SIZE
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.makePortFromRunFilenameExtractor
import org.jetbrains.kotlin.daemon.common.readRunFileSecret
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.net.InetAddress
import java.net.Socket
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("KT-90353: un-authenticated connections to the daemon are rejected")
class DaemonConnectionAuthTest : BaseDaemonSessionTest() {
    private fun findSingleRunFile(): Pair<File, Int> {
        val portExtractor = makePortFromRunFilenameExtractor(digest = "[0-9a-f]+")
        val runFiles = File(defaultDaemonOptions.runFilesPath).listFiles().orEmpty()
            .mapNotNull { file -> portExtractor(file.name)?.let { port -> file to port } }
        assertEquals(1, runFiles.size, "Expected exactly one daemon run file, found: $runFiles")
        return runFiles.single()
    }

    @DisplayName("The run file holds the secret and is readable only by the owner")
    @Test
    fun runFileHoldsOwnerOnlySecret() {
        leaseSession()
        val runFile = findSingleRunFile().first

        val content = runFile.readText().trim()
        assertEquals(DAEMON_AUTH_SECRET_SIZE * 2, content.length, "Unexpected run file content: '$content'")
        assertTrue(content.all { it in "0123456789abcdef" }, "Unexpected run file content: '$content'")
        assertNotNull(readRunFileSecret(runFile))

        if (Files.getFileStore(runFile.toPath()).supportsFileAttributeView("posix")) {
            assertEquals(
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                Files.getPosixFilePermissions(runFile.toPath())
            )
        }
    }

    @DisplayName("A plain RMI client without the handshake cannot look up the service")
    @Test
    fun plainRmiClientIsRejected() {
        leaseSession()
        val port = findSingleRunFile().second

        // the default socket factory sends the RMI header right away instead of the handshake
        val registry = LocateRegistry.getRegistry(LoopbackNetworkInterface.loopbackInetAddressName, port)
        assertThrows<RemoteException> { registry.lookup(COMPILER_SERVICE_RMI_NAME) }
    }

    @DisplayName("A connection with a wrong secret is closed without a reply")
    @Test
    fun wrongSecretIsRejected() {
        leaseSession()
        val port = findSingleRunFile().second

        Socket(InetAddress.getByName(null), port).use { socket ->
            socket.soTimeout = 30_000
            val out = socket.getOutputStream()
            out.write("KCD1".toByteArray(Charsets.US_ASCII))
            out.write(ByteArray(DAEMON_AUTH_SECRET_SIZE) { 0x42 })
            out.write("JRMI".toByteArray(Charsets.US_ASCII))
            out.flush()
            val reply = runCatching { socket.getInputStream().read() }
            assertTrue(reply.isFailure || reply.getOrNull() == -1, "Expected the connection to be closed, got reply: $reply")
        }
    }
}
