package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.io.ByteBuffer
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.io.copyAndClose
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.io.core.readBytes
import java.io.*
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
            } catch (e: IOException) {
                log.info("failed to read message length, ${e.message}")
                null
            }

    suspend fun readPacket(length: Int, readChannel: ByteReadChannel) =
        try {
            readChannel.readPacket(
                length
            ).readBytes()
        } catch (e: IOException) {
            log.info("failed to read packet (${e.message})")
            null
        }

    private val readActor = actor<ReadQuery>(capacity = Channel.UNLIMITED) {
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
                println("read chanel closed " + log.name)
            }
        }
    }

    private fun getLength(packet: ByteArray): Int {
        val (b1, b2, b3, b4) = packet.map(Byte::toInt)
        return (0xFF and b1 shl 24 or (0xFF and b2 shl 16) or
                (0xFF and b3 shl 8) or (0xFF and b4)).also { log.info("   $it") }
    }

    /** reads exactly <tt>length</tt>  bytes.
     * after deafault timeout returns <tt>DEFAULT_BYTE_ARRAY</tt> */
    suspend fun readBytes(length: Int): ByteArray = runBlockingWithTimeout {
        val expectedBytes = CompletableDeferred<ByteArray>()
//        readActor.send(GivenLengthBytesQuery(length, expectedBytes))
        expectedBytes.await()
    } ?: DEFAULT_BYTE_ARRAY

    /** first reads <t>length</t> token (4 bytes) and then -- reads <t>length</t> bytes.
     * after deafault timeout returns <tt>DEFAULT_BYTE_ARRAY</tt> */
    suspend fun nextBytes(): ByteArray = runBlockingWithTimeout {
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
    suspend fun nextObject() = runBlocking {
        val obj = CompletableDeferred<Any?>()
        readActor.send(SerObjectQuery(obj))
        val result = obj.await()
        if (result is Server.ServerDownMessage<*>) {
            throw IOException("connection closed by server")
        }
        result
    }

}


class ByteWriteChannelWrapper(writeChannel: ByteWriteChannel, private val log: Logger) {

    private interface WriteActorQuery

    private open class ByteData(val bytes: ByteArray): WriteActorQuery {
        open fun toByteArray(): ByteArray = bytes
    }

    private class ObjectWithLength(val lengthBytes: ByteArray, bytes: ByteArray) : ByteData(bytes) {
        override fun toByteArray() = lengthBytes + bytes
    }

    private class CloseMessage: WriteActorQuery

    private suspend fun tryPrint(b: Byte, writeChannel: ByteWriteChannel) {
        if (!writeChannel.isClosedForWrite) {
            try {
                writeChannel.writeByte(b)
            } catch (e: IOException) {
                log.info("failed to print message, ${e.message}")
            }
        } else {
            log.info("closed chanel (write)")
        }
    }

    private val writeActor = actor<WriteActorQuery>(capacity = Channel.UNLIMITED) {
        consumeEach { message ->
            if (!writeChannel.isClosedForWrite) {
                when (message) {
                    is CloseMessage -> {
                        log.info("${log.name} closing chanel...")
                        writeChannel.close()
                    }
                    is ByteData -> {
                        message.toByteArray().forEach {
                            tryPrint(it, writeChannel)
                        }
                        if (!writeChannel.isClosedForWrite) {
                            try {
                                writeChannel.flush()
                            } catch (e: IOException) {
                                log.info("failed to flush byte write chanel")
                            }
                        }
                    }
                }
            } else {
                println("${log.name} write chanel closed")
            }
        }
    }

    suspend fun printBytesAndLength(length: Int, bytes: ByteArray) {
        writeActor.send(
            ObjectWithLength(
                getLengthBytes(length),
                bytes
            )
        )
    }

//    suspend fun printBytes(bytes: ByteArray) {
//        writeActor.send(ByteData(bytes))
//    }

    private suspend fun printObjectImpl(obj: Any?) =
        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { objOut ->
                objOut.writeObject(obj)
                objOut.flush()
                val bytes = bos.toByteArray()
                printBytesAndLength(bytes.size, bytes)
            }
        }
            .also {
                log.info("sent object : $obj")
            }

    private suspend fun printString(s: String) = printBytesAndLength(-s.length, s.toByteArray())

    fun getLengthBytes(length: Int) =
        ByteBuffer
            .allocate(4)
            .putInt(length)
            .array()
            .also {
                log.info("printLength $length")
            }

    suspend fun writeObject(obj: Any?) {
        if (obj is String) printString(obj)
        else printObjectImpl(obj)
    }

    fun close() {
        runBlocking {
            writeActor.send(CloseMessage())
        }
    }
}

fun ByteReadChannel.toWrapper(log: Logger) = ByteReadChannelWrapper(this, log)
fun ByteWriteChannel.toWrapper(log: Logger) = ByteWriteChannelWrapper(this, log)

fun Socket.openAndWrapReadChannel(log: Logger) = this.openReadChannel().toWrapper(log)
fun Socket.openAndWrapWriteChannel(log: Logger) = this.openWriteChannel().toWrapper(log)

data class IOPair(val input: ByteReadChannelWrapper, val output: ByteWriteChannelWrapper)

fun Socket.openIO(log: Logger) = IOPair(this.openAndWrapReadChannel(log), this.openAndWrapWriteChannel(log))