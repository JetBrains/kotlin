// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) {
    fun test() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long) {
    fun test() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String) {
    fun test() = x
}

fun box(): String {
    if (Z::test.let { it.invoke(Z(42)) } != 42) throw AssertionError()
    if (L::test.let { it.invoke(L(1234L)) } != 1234L) throw AssertionError()
    if (S::test.let { it.invoke(S("abcdef")) } != "abcdef") throw AssertionError()

    return "OK"
}