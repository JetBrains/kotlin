// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Z(val z: Int)

interface ITest {
    fun testDefault(z: Z = Z(42)) = z.z

    fun testOverridden(z: Z = Z(42)): Int
}

class Test : ITest {
    override fun testOverridden(z: Z) = z.z
}

fun box(): String {
    if (Test().testDefault() != 42) throw AssertionError()
    if (Test().testDefault(Z(123)) != 123) throw AssertionError()

    if (Test().testOverridden() != 42) throw AssertionError()
    if (Test().testOverridden(Z(123)) != 123) throw AssertionError()

    return "OK"
}