// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String)

fun test(aZ: Z, aL: L, aS: S) = "${aZ.x} ${aL.x} ${aS.x}"

fun box(): String {
    if (::test.let { it.invoke(Z(1), L(1L), S("abc")) } != "1 1 abc") throw AssertionError()

    return "OK"
}