// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val value: T)

fun eq(a: Z<Int>?, b: Z<Int>) = a == b

fun eqq(a: Z<Int>, b: Z<Int>?) = a == b

fun box(): String {
    if (!eq(Z(1), Z(1))) throw AssertionError()
    if (eq(Z(1), Z(2))) throw AssertionError()
    if (eq(null, Z(0))) throw AssertionError()

    if (!eqq(Z(1), Z(1))) throw AssertionError()
    if (eqq(Z(1), Z(2))) throw AssertionError()
    if (eqq(Z(0), null)) throw AssertionError()

    return "OK"
}