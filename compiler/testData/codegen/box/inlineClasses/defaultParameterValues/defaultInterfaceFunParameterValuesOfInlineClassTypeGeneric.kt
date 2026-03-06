// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val z: T)

interface ITest {
    fun testDefault(z: Z<Int> = Z(42)) = z.z

    fun testOverridden(z: Z<Int> = Z(42)): Int
}

class Test : ITest {
    override fun testOverridden(z: Z<Int>) = z.z
}

fun box(): String {
    if (Test().testDefault() != 42) throw AssertionError()
    if (Test().testDefault(Z(123)) != 123) throw AssertionError()

    if (Test().testOverridden() != 42) throw AssertionError()
    if (Test().testOverridden(Z(123)) != 123) throw AssertionError()

    return "OK"
}