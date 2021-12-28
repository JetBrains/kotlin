// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IFoo<T : IFoo<T>> {
    fun foo(t: T): String = t.bar()
    fun bar(): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T) : IFoo<Z<Int>> {
    override fun bar(): String = "OK"
}

fun box(): String =
    Z(1).foo(Z(2))
