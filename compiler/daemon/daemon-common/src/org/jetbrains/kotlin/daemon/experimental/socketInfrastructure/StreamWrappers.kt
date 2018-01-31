package org.jetbrains.kotlin.daemon.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.experimental.io.ByteBuffer
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.io.core.readBytes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


class ByteReadChannelWrapper(private val readChannel: ByteReadChannel) {

    private suspend fun readBytes(length: Int) =
            readChannel.readPacket(length).readBytes()

    private suspend fun getObject(length: Int) =
            if (length >= 0) {
                ObjectInputStream(
                        ByteArrayInputStream(readBytes(length))
                ).use {
                    it.readObject()
                }
            } else { // optimize for long strings!
                String(ByteArrayInputStream(
                        readBytes(-length)
                ).readBytes())
            }

    private suspend fun getLength(): Int {
        val packet = readBytes(4)
        val b1 = packet[0].toInt()
        val b2 = packet[1].toInt()
        val b3 = packet[2].toInt()
        val b4 = packet[3].toInt()
        return 0xFF and b1 shl 24 or (0xFF and b2 shl 16) or
                (0xFF and b3 shl 8) or (0xFF and b4)
    }

    suspend fun nextObject() = getObject(getLength())

}


class ByteWriteChannelWrapper(private val writeChannel: ByteWriteChannel) {

    private suspend fun printBytes(bytes: ByteArray) {
        bytes.forEach { writeChannel.writeByte(it) }
        writeChannel.flush()
    }

    private suspend fun printObjectImpl(obj: Any) =
            ByteArrayOutputStream().use { bos ->
                ObjectOutputStream(bos).use { objOut ->
                    objOut.writeObject(obj)
                    objOut.flush()
                    val bytes = bos.toByteArray()
                    printLength(bytes.size)
                    printBytes(bytes)
                }
            }

    private suspend fun printString(s: String) {
        printLength(-s.length)
        printBytes(s.toByteArray())
    }


    private suspend fun printLength(length: Int) = printBytes(
            ByteBuffer
                    .allocate(4)
                    .putInt(length)
                    .array()
    )

    suspend fun writeObject(obj: Any) =
            if (obj is String) printString(obj)
            else printObjectImpl(obj)
}

fun ByteReadChannel.toWrapper() = ByteReadChannelWrapper(this)
fun ByteWriteChannel.toWrapper() = ByteWriteChannelWrapper(this)

fun Socket.openAndWrapReadChannel() = this.openReadChannel().toWrapper()
fun Socket.openAndWrapWriteChannel() = this.openWriteChannel().toWrapper()