// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) {
    fun test(y: Int = 42) = x + y
}

fun box(): String {
    if (Z(800).test() != 842) throw AssertionError()
    if (Z(400).test(32) != 432) throw AssertionError()

    return "OK"
}