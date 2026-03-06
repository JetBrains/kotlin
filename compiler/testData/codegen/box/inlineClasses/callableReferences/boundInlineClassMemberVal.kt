// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) {
    val xx get() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long) {
    val xx get() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String) {
    val xx get() = x
}

fun box(): String {
    if (Z(42)::xx.get() != 42) throw AssertionError()
    if (L(1234L)::xx.get() != 1234L) throw AssertionError()
    if (S("abcdef")::xx.get() != "abcdef") throw AssertionError()

    return "OK"
}