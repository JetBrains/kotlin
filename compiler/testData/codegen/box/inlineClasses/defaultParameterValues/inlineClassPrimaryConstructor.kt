// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int = 1234)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long = 1234L)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String = "foobar")

fun box(): String {
    if (Z().x != 1234) throw AssertionError()
    if (L().x != 1234L) throw AssertionError()
    if (S().x != "foobar") throw AssertionError()

    return "OK"
}