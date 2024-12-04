// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: Any)

interface IFoo<T> {
    fun foo(): T
}

class TestX : IFoo<X> {
    override fun foo(): X = X("OK")
}

fun box(): String {
    val t: IFoo<X> = TestX()
    return ((t.foo() as Any) as X).x.toString()
}
