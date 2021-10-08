// WITH_RUNTIME

interface IFoo<T : IFoo<T>> {
    fun foo(t: T): String = t.bar()
    fun bar(): String
}

@JvmInline
value class Z(val x: Int) : IFoo<Z> {
    override fun bar(): String = "OK"
}

fun box(): String =
    Z(1).foo(Z(2))
