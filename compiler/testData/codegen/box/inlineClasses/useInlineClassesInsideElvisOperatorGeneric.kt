// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(private val u: T) {
    fun asResult() = u
}

fun <T: Int> test(x1: UInt<T>?, x2: UInt<T>?, y: UInt<T>, z: UInt<T>?): Int {
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
