// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: Char>(val x: T)

interface IFoo {
    fun foo(): Any
    fun bar(): X<Char>
}

class TestX : IFoo {
    override fun foo(): X<Char> = X('O')
    override fun bar(): X<Char> = X('K')
}

fun box(): String {
    val t: IFoo = TestX()
    val tFoo = t.foo()
    if (tFoo !is X<*>) {
        throw AssertionError("X expected: $tFoo")
    }

    return (t.foo() as X<Char>).x.toString() + t.bar().x.toString()
}