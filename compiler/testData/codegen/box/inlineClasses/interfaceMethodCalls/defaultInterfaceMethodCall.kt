// WITH_RUNTIME

interface IFoo {
    fun foo() = bar()
    fun bar(): String
}

@JvmInline
value class Z(val x: Int) : IFoo {
    override fun bar(): String = "OK"
}

@JvmInline
value class L(val x: Long) : IFoo {
    override fun bar(): String = "OK"
}

@JvmInline
value class S(val x: String) : IFoo {
    override fun bar(): String = "OK"
}

fun box(): String {
    if (Z(42).foo() != "OK") throw AssertionError()
    if (L(4L).foo() != "OK") throw AssertionError()
    if (S("").foo() != "OK") throw AssertionError()

    return "OK"
}