// IGNORE_BACKEND: JVM_IR
inline fun <T> runAfterLoop(fn: () -> T): T {
    for (i in 1..2);
    return fn()
}

inline fun bar(x: Int, y: Long, z: Byte, s: String) = runAfterLoop { x.toString() + y.toString() + z.toString() + s }

fun foobar(x: Int, y: Long, s: String, z: Byte) = x.toString() + y.toString() + s + z.toString()

fun foo() : String {
    return foobar(1, 2L, bar(3, 4L, 5.toByte(), "6"), 7.toByte())
}

// fake inline variables occupy 7 ISTOREs.
// 16 ISTORE
// 13 ILOAD
// 2 ASTORE
// 8 ALOAD
// 2 LSTORE
// 4 LLOAD
// 1 MAXLOCALS = 10
// 0 InlineMarker
