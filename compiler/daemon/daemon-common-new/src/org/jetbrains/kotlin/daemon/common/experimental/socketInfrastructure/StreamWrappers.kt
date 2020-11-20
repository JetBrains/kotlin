/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.io.*
import kotlinx.io.core.readBytes
import java.io.*
import java.nio.ByteBuffer
import java.util.logging.Logger

private val DEFAULT_BYTE_ARRAY = byteArrayOf(0, 0, 0, 0)

class ByteReadChannelWrapper(readChannel: ByteReadChannel, private val log: Logger) {

    private interface ReadQuery

    private open class BytesQuery(val bytes: CompletableDeferred<ByteArray>) : ReadQuery

    private class SerObjectQuery(val obj: CompletableDeferred<Any?>) : ReadQuery

    suspend fun readLength(readChannel: ByteReadChannel) =
        if (readChannel.isClosedForRead)
            null
        else
            try {
                readChannel.readPacket(4).readBytes()
            } catch (e: Exception) {
                log.fine("failed to read message length, ${e.message}")
                null
            }

    suspend fun readPacket(length: Int, readChannel: ByteReadChannel) =
        try {
            readChannel.readPacket(
                length
            ).readBytes()
        } catch (e: Exception) {
            log.fine("failed to read packet (${e.message})")
            null
        }

    // TODO : replace GlobalScope with something more explicit here and below.
    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val readActor = GlobalScope.actor<ReadQuery>(capacity = Channel.UNLIMITED) {
        consumeEach { message ->
            if (!readChannel.isClosedForRead) {
                readLength(readChannel)?.let { messageLength ->
                    when (message) {
                        is BytesQuery -> message.bytes.complete(
                            readChannel.readPacket(
                                getLength(messageLength)
                            ).readBytes()
                        )

                        is SerObjectQuery -> message.obj.complete(
                            getObject(
                                getLength(messageLength),
                                { len -> readPacket(len, readChannel) }
                            )
                        )

                        else -> {
                        }
                    }
                }
            } else {
                log.fine("read chanel closed " + log.name)
            }
        }
    }

    private fun getLength(packet: ByteArray): Int {
        val (b1, b2, b3, b4) = packet.map(Byte::toInt)
        return (0xFF and b1 shl 24 or (0xFF and b2 shl 16) or
                (0xFF and b3 shl 8) or (0xFF and b4)).also { log.fine("   $it") }
    }

    /** first reads <t>length</t> token (4 bytes) and then -- reads <t>length</t> bytes.
     * after deafault timeout returns <tt>DEFAULT_BYTE_ARRAY</tt> */
    suspend fun nextBytes(): ByteArray = runWithTimeout {
        val expectedBytes = CompletableDeferred<ByteArray>()
        readActor.send(BytesQuery(expectedBytes))
        expectedBytes.await()
    } ?: DEFAULT_BYTE_ARRAY

    private suspend fun getObject(length: Int, readPacket: suspend (Int) -> ByteArray?): Any? =
        if (length >= 0) {
            readPacket(length)?.let { bytes ->
                ObjectInputStream(
                    ByteArrayInputStream(bytes)
                ).use {
                    it.readObject()
                }
            }
        } else { // optimize for long strings!
            readPacket(-length)?.let { bytes ->
                String(
                    ByteArrayInputStream(
                        bytes
                    ).readBytes()
                )
            }
        }

    /** first reads <t>length</t> token (4 bytes), then reads <t>length</t> bytes and returns deserialized object */
    suspend fun nextObject(): Any? {
        val obj = CompletableDeferred<Any?>()
        readActor.send(SerObjectQuery(obj))
        val result = obj.await()
        if (result is Server.ServerDownMessage<*>) {
            throw IOException("connection closed by server")
        }
        return result
    }

}

class ByteWriteChannelWrapper(writeChannel: ByteWriteChannel, private val log: Logger) {

    private interface WriteActorQuery

    private open class ByteData(val bytes: ByteArray) : WriteActorQuery {
        open fun toByteArray(): ByteArray = bytes
    }

    private class ObjectWithLength(val lengthBytes: ByteArray, bytes: ByteArray) : ByteData(bytes) {
        override fun toByteArray() = lengthBytes + bytes
    }

    private class CloseMessage : WriteActorQuery

    private suspend fun tryWrite(b: ByteArray, writeChannel: ByteWriteChannel) {
        if (!writeChannel.isClosedForWrite) {
            try {
                writeChannel.writeFully(b)
            } catch (e: Exception) {
                log.fine("failed to print message, ${e.message}")
            }
        } else {
            log.fine("closed chanel (write)")
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val writeActor = GlobalScope.actor<WriteActorQuery>(capacity = Channel.UNLIMITED) {
        consumeEach { message ->
            if (!writeChannel.isClosedForWrite) {
                when (message) {
                    is CloseMessage -> {
                        log.fine("${log.name} closing chanel...")
                        writeChannel.close()
                    }
                    is ByteData -> {
                        tryWrite(message.toByteArray(), writeChannel)
                        if (!writeChannel.isClosedForWrite) {
                            try {
                                writeChannel.flush()
                            } catch (e: Exception) {
                                log.fine("failed to flush byte write chanel")
                            }
                        }
                    }
                }
            } else {
                log.fine("${log.name} write chanel closed")
            }
        }
    }

    suspend fun writeBytesAndLength(length: Int, bytes: ByteArray) {
        writeActor.send(
            ObjectWithLength(
                getLengthBytes(length),
                bytes
            )
        )
    }

    private suspend fun writeObjectImpl(obj: Any?) =
        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { objOut ->
                objOut.writeObject(obj)
                objOut.flush()
                val bytes = bos.toByteArray()
                writeBytesAndLength(bytes.size, bytes)
            }
        }

    private suspend fun writeString(s: String) = writeBytesAndLength(-s.length, s.toByteArray())

    fun getLengthBytes(length: Int) =
        ByteBuffer
            .allocate(4)
            .putInt(length)
            .array()

    suspend fun writeObject(obj: Any?) {
        if (obj is String) writeString(obj)
        else writeObjectImpl(obj)
    }

    suspend fun close() = writeActor.send(CloseMessage())

}

fun ByteReadChannel.toWrapper(log: Logger) = ByteReadChannelWrapper(this, log)
fun ByteWriteChannel.toWrapper(log: Logger) = ByteWriteChannelWrapper(this, log)

fun Socket.openAndWrapReadChannel(log: Logger) = this.openReadChannel().toWrapper(log)
fun Socket.openAndWrapWriteChannel(log: Logger) = this.openWriteChannel().toWrapper(log)

data class IOPair(val input: ByteReadChannelWrapper, val output: ByteWriteChannelWrapper)

fun Socket.openIO(log: Logger) = IOPair(this.openAndWrapReadChannel(log), this.openAndWrapWriteChannel(log))