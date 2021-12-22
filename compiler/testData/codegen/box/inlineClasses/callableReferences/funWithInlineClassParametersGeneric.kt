// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val x: T)

fun test(aZ: Z<Int>, aL: L<Long>, aS: S<String>) = "${aZ.x} ${aL.x} ${aS.x}"

fun box(): String {
    if (::test.invoke(Z(1), L(1L), S("abc")) != "1 1 abc") throw AssertionError()

    return "OK"
}