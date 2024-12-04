// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(val value: T)

fun <T> takeVarargs(vararg e: T): T {
    return e[e.size - 1]
}

fun <T: Int> test(u1: UInt<T>, u2: UInt<T>, u3: UInt<T>?): Int {
    val a = takeVarargs(u1, u2)
    val b = takeVarargs(u3) ?: UInt(-1)
    val c = takeVarargs(u1, u3) ?: UInt(-1)

    return a.value + b.value + c.value
}

fun box(): String {
    val u1 = UInt(0)
    val u2 = UInt(1)
    val u3 = UInt(2)
    if (test(u1, u2, u3) != 1 + 2 + 2) return "fail"

    return "OK"
}