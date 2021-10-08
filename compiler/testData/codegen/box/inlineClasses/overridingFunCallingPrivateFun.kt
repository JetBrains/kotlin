// WITH_RUNTIME

interface IFoo {
    fun foo(): String
}

@JvmInline
value class IC(val x: String) : IFoo {
    private fun privateFun() = x
    override fun foo() = privateFun()
}

fun box(): String {
    val x: IFoo = IC("OK")
    return x.foo()
}