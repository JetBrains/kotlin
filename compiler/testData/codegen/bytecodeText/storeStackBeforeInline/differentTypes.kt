inline fun bar(x: Int, y: Long, z: Byte, s: String) = x.toString() + y.toString() + z.toString() + s

fun foobar(x: Int, y: Long, s: String, z: Byte) = x.toString() + y.toString() + s + z.toString()

fun foo() : String {
    return foobar(1, 2L, bar(3, 4L, 5.toByte(), "6"), 7.toByte())
}

// 3 ISTORE
// 7 ILOAD
// 2 ASTORE
// 6 ALOAD
// 2 LSTORE
// 4 LLOAD
// 1 MAXLOCALS = 10
// 0 InlineMarker
