// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val x: T)

fun box(): String {
    if (Z(42)::x.get() != 42) throw AssertionError()
    if (L(1234L)::x.get() != 1234L) throw AssertionError()
    if (S("abcdef")::x.get() != "abcdef") throw AssertionError()

    return "OK"
}