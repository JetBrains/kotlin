// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val x: T)

fun Z<Int>.test() = x
fun L<Long>.test() = x
fun S<String>.test() = x

fun box(): String {
    if (Z<Int>::test.let { it.invoke(Z(42)) } != 42) throw AssertionError()
    if (L<Long>::test.let { it.invoke(L(1234L)) } != 1234L) throw AssertionError()
    if (S<String>::test.let { it.invoke(S("abcdef")) } != "abcdef") throw AssertionError()

    return "OK"
}