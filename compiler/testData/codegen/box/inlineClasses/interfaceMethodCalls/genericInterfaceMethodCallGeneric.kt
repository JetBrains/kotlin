// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IFoo<T> {
    fun foo(x: T): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T) : IFoo<Z<Int>> {
    override fun foo(x: Z<Int>) = "OK"
}

fun box(): String =
    Z(1).foo(Z(2))