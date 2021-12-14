// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val z: Int)

class Test(val z: Z = Z(42)) {
    fun test() = z.z
}

fun box(): String {
    if (Test().test() != 42) throw AssertionError()
    if (Test(Z(123)).test() != 123) throw AssertionError()

    return "OK"
}