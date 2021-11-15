// WITH_STDLIB

interface IFoo<T> {
    fun foo(x: T): String = "O"
    fun T.bar(): String = "K"
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class L(val x: Long) : IFoo<L>

fun box(): String {
    val z = L(0L)
    return with(z) {
        foo(z) + z.bar()
    }
}