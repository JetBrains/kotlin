import sun.nio.ch.DirectBuffer

fun test(buffer: DirectBuffer): Long = buffer.address()
