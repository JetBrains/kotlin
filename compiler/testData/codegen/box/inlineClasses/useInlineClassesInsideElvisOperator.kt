// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class UInt(private val u: Int) {
    fun asResult() = u
}

fun test(x1: UInt?, x2: UInt?, y: UInt, z: UInt?): Int {
    val a = x1 ?: y
    val b = x1 ?: z!!
    val c = x1 ?: x2 ?: y
    return a.asResult() + b.asResult() + c.asResult()
}

fun box(): String {
    val u1 = UInt(10)
    val u2 = UInt(20)
    val r = test(null, null, u1, u2)
    return if (r != 40) "fail: $r" else "OK"
}
