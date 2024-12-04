// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) {
    val xx get() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long) {
    val xx get() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String) {
    val xx get() = x
}

fun box(): String {
    if ((Z::xx).get(Z(42)) != 42) throw AssertionError()
    if ((L::xx).get(L(1234L)) != 1234L) throw AssertionError()
    if ((S::xx).get(S("abcdef")) != "abcdef") throw AssertionError()

    return "OK"
}