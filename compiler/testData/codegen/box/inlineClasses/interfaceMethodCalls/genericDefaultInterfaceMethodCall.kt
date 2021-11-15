// WITH_STDLIB

interface IFoo<T : IFoo<T>> {
    fun foo(t: T): String = t.bar()
    fun bar(): String
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int) : IFoo<Z> {
    override fun bar(): String = "OK"
}

fun box(): String =
    Z(1).foo(Z(2))
