// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(val s: T)

fun <T: Int> test(a1: Any, a2: UInt<T>?, a3: Any?, a4: Any?): Int {
    val b1 = a1 as UInt<T>
    val b2 = a2 as UInt<T>
    val b3 = (a3 as UInt<T>?) as UInt<T>
    val b4 = (a4 as? UInt<T>) as UInt<T>
    return b1.s + b2.s + b3.s + b4.s
}

fun box(): String {
    val u1 = UInt(1)
    val u2 = UInt(2)
    if (test(u1, u2, u1, u2) != 6) return "fail"

    return "OK"
}