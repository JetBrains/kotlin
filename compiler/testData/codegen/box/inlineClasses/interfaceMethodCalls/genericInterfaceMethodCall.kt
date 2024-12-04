// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface IFoo<T> {
    fun foo(x: T): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) : IFoo<Z> {
    override fun foo(x: Z) = "OK"
}

fun box(): String =
    Z(1).foo(Z(2))