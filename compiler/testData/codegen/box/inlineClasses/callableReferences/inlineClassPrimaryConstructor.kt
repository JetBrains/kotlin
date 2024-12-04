// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String)

fun box(): String {
    if (42.let(::Z).x != 42) throw AssertionError()
    if (1234L.let(::L).x != 1234L) throw AssertionError()
    if ("abcdef".let(::S).x != "abcdef") throw AssertionError()

    return "OK"
}