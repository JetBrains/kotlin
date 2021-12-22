// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T) {
    val xx get() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val x: T) {
    val xx get() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val x: T) {
    val xx get() = x
}

fun box(): String {
    if (Z(42)::xx.get() != 42) throw AssertionError()
    if (L(1234L)::xx.get() != 1234L) throw AssertionError()
    if (S("abcdef")::xx.get() != "abcdef") throw AssertionError()

    return "OK"
}