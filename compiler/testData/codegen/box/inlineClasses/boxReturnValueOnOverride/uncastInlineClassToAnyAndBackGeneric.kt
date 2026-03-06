// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: Any>(val x: T)

interface IFoo<T> {
    fun foo(): T
}

class TestX : IFoo<X<String>> {
    override fun foo(): X<String> = X("OK")
}

fun box(): String {
    val t: IFoo<X<String>> = TestX()
    return ((t.foo() as Any) as X<*>).x.toString()
}
