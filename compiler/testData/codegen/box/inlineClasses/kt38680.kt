// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val s: String)

interface IFoo<T> {
    fun foo(x: T, s: String = "K"): String
}

class FooImpl : IFoo<IC> {
    override fun foo(x: IC, s: String): String = x.s + s
}

fun box(): String = FooImpl().foo(IC("O"))