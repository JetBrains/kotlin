interface IFoo<T> {
    fun foo(x: T): String = "OK"
    fun T.bar(): String = "OK"
    fun withDefault(x: T, y: Int = 42): String = "OK"
}

inline class L(val x: Long) : IFoo<L>

class X : IFoo<L>