// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T) {
    fun test(y: Int = 42) = x + y
}

fun box(): String {
    if (Z(800).test() != 842) throw AssertionError()
    if (Z(400).test(32) != 432) throw AssertionError()

    return "OK"
}