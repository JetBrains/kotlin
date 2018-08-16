import kotlinx.cinterop.*
import platform.zlib.*

val source = immutableBinaryBlobOf(0xF3, 0x48, 0xCD, 0xC9, 0xC9, 0x57, 0x04, 0x00).asCPointer(0)!!
val golden = immutableBinaryBlobOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21, 0x00).asCPointer(0)!!

fun main(args: Array<String>) = memScoped {
    val buffer = allocArray<ByteVar>(32)

    val z = alloc<z_stream>().apply {
        next_in   = source
        avail_in  = 8
        next_out  = buffer
        avail_out = 32
    }.ptr

    if (inflateInit2(z, -15) == Z_OK && inflate(z, Z_FINISH) == Z_STREAM_END && inflateEnd(z) == Z_OK) 
        println(buffer.toKString())

    println(golden.toKString())
}
