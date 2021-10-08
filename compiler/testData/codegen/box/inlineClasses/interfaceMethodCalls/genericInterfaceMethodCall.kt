// WITH_RUNTIME

interface IFoo<T> {
    fun foo(x: T): String
}

@JvmInline
value class Z(val x: Int) : IFoo<Z> {
    override fun foo(x: Z) = "OK"
}

fun box(): String =
    Z(1).foo(Z(2))