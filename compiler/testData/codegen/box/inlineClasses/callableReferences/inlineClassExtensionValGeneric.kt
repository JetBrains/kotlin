// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val x: T)

val Z<Int>.xx get() = x
val L<Long>.xx get() = x
val S<String>.xx get() = x

fun box(): String {
    if ((Z<Int>::xx).get(Z(42)) != 42) throw AssertionError()
    if ((L<Long>::xx).get(L(1234L)) != 1234L) throw AssertionError()
    if ((S<String>::xx).get(S("abcdef")) != "abcdef") throw AssertionError()

    return "OK"
}