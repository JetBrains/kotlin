// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val s: String)

interface IFoo<T> {
    fun foo(x: T, s: String = "K"): String
}

interface IFoo2<T> : IFoo<T> {
    fun bar(x: T) = foo(x)
}

class FooImpl : IFoo2<IC> {
    override fun foo(x: IC, s: String): String = x.s + s
}

fun box(): String = FooImpl().bar(IC("O"))