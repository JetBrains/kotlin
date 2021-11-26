// WITH_STDLIB

interface IBase {
    fun testDefault1() = if (this is B) this.foo() else "fail"
}

interface IFoo : IBase {
    fun foo(): String

    fun testDefault2() = if (this is B) this.foo() else "fail"
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class B(val x: String) : IFoo {
    override fun foo() = x
}

fun IBase.test1() = if (this is IFoo) foo() else "fail"

fun IBase.test2() = if (this is B) foo() else "fail"

fun box(): String {
    if (B("OK").test1() != "OK") throw AssertionError()
    if (B("OK").test2() != "OK") throw AssertionError()
    if (B("OK").testDefault1() != "OK") throw AssertionError()
    if (B("OK").testDefault2() != "OK") throw AssertionError()

    return "OK"
}