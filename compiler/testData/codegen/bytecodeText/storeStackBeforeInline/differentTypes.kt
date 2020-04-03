inline fun <T> runAfterLoop(fn: () -> T): T {
    for (i in 1..2);
    return fn()
}

inline fun bar(x: Int, y: Long, z: Byte, s: String) = runAfterLoop { x.toString() + y.toString() + z.toString() + s }

fun foobar(x: Int, y: Long, s: String, z: Byte) {}

fun foo() {
    foobar(1, 2L, bar(3, 4L, 5.toByte(), "6"), 7.toByte())
}

// 2 ASTORE
// 7 ALOAD
// 2 LSTORE
// 3 LLOAD
// 1 MAXLOCALS = 10
// 0 InlineMarker

// JVM_TEMPLATES
// fake inline variables occupy 7 ISTOREs.
// 16 ISTORE
// 11 ILOAD

// JVM_IR_TEMPLATES
// JVM_IR generates an extra induction variable in 'for (i in 1..2)' (see KT-36837)
// 19 ISTORE
// 14 ILOAD
