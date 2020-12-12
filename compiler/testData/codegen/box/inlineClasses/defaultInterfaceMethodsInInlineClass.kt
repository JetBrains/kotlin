interface IFoo<T> {
    fun foo(x: T): String = "O"
    fun T.bar(): String = "K"
}

inline class L(val x: Long) : IFoo<L>

fun box(): String {
    val z = L(0L)
    return with(z) {
        foo(z) + z.bar()
    }
}