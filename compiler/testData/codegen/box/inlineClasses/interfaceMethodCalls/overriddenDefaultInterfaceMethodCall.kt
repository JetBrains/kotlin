// WITH_RUNTIME

interface IBase {
    fun foo() = "BAD"
}

interface IFoo : IBase {
    override fun foo() = "OK"
}

@JvmInline
value class Z(val x: Int) : IFoo

@JvmInline
value class L(val x: Long) : IFoo

@JvmInline
value class S(val x: String) : IFoo

fun box(): String {
    if (Z(42).foo() != "OK") throw AssertionError()
    if (L(4L).foo() != "OK") throw AssertionError()
    if (S("").foo() != "OK") throw AssertionError()

    return "OK"
}