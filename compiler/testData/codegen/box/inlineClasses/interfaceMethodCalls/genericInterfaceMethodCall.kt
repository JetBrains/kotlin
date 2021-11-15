// WITH_STDLIB

interface IFoo<T> {
    fun foo(x: T): String
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int) : IFoo<Z> {
    override fun foo(x: Z) = "OK"
}

fun box(): String =
    Z(1).foo(Z(2))