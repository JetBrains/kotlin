// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T) {
    fun test() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val x: T) {
    fun test() = x
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val x: T) {
    fun test() = x
}

fun box(): String {
    if (Z<Int>::test.invoke(Z(42)) != 42) throw AssertionError()
    if (L<Long>::test.invoke(L(1234L)) != 1234L) throw AssertionError()
    if (S<String>::test.invoke(S("abcdef")) != "abcdef") throw AssertionError()

    return "OK"
}