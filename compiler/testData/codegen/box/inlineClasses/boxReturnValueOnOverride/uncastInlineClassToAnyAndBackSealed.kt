// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: Any): IC()

interface IFoo<T> {
    fun foo(): T
}

class TestX : IFoo<IC> {
    override fun foo(): IC = X("OK")
}

fun box(): String {
    val t: IFoo<IC> = TestX()
    return ((t.foo() as Any) as X).x.toString()
}
