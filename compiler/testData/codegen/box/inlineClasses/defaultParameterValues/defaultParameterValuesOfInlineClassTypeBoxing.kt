// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

interface IFoo {
    fun foo(): String
}

inline class Z(val z: Int) : IFoo {
    override fun foo() = z.toString()
}

fun testNullable(z: Z? = Z(42)) = z!!.z

fun testAny(z: Any = Z(42)) = (z as Z).z

fun testInterface(z: IFoo = Z(42)) = z.foo()

fun box(): String {
    if (testNullable() != 42) throw AssertionError()
    if (testNullable(Z(123)) != 123) throw AssertionError()

    if (testAny() != 42) throw AssertionError()
    if (testAny(Z(123)) != 123) throw AssertionError()

    if (testInterface() != "42") throw AssertionError()
    if (testInterface(Z(123)) != "123") throw AssertionError()

    return "OK"
}