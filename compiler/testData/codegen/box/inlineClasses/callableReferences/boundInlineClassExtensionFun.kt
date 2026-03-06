// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String)

fun Z.test() = x
fun L.test() = x
fun S.test() = x

fun box(): String {
    if (Z(42)::test.let { it.invoke() } != 42) throw AssertionError()
    if (L(1234L)::test.let { it.invoke() } != 1234L) throw AssertionError()
    if (S("abcdef")::test.let { it.invoke() } != "abcdef") throw AssertionError()

    return "OK"
}