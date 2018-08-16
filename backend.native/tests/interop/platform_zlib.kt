import kotlinx.cinterop.*

import platform.zlib.*

val source = immutableBinaryBlobOf(0xF3, 0x48, 0xCD, 0xC9, 0xC9, 0x57, 0x04, 0x00).asCPointer()
val golden = immutableBinaryBlobOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21, 0x00).asCPointer()

fun main(args: Array<String>) = memScoped {
    val buffer = ByteArray(32)
    buffer.usePinned { pinned ->
        val z = alloc<z_stream>().apply {
            next_in = source
            avail_in = 8
            next_out = pinned.addressOf(0)
            avail_out = buffer.size
        }.ptr

        if (inflateInit2(z, -15) == Z_OK && inflate(z, Z_FINISH) == Z_STREAM_END && inflateEnd(z) == Z_OK)
            println(buffer.stringFromUtf8())
    }
    println(golden.toKString())
}
