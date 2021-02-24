// !LANGUAGE: +InlineClasses

inline class IC(val s: String)

interface IFoo<T> {
    fun foo(x: T, s: String = "K"): String

    fun bar(x: T) = foo(x)
}

class FooImpl : IFoo<IC> {
    override fun foo(x: IC, s: String): String = x.s + s
}

fun box(): String = FooImpl().bar(IC("O"))