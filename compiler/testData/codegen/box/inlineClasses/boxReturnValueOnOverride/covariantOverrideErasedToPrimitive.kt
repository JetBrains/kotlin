// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: Char)

interface IFoo {
    fun foo(): Any
    fun bar(): X
}

class TestX : IFoo {
    override fun foo(): X = X('O')
    override fun bar(): X = X('K')
}

fun box(): String {
    val t: IFoo = TestX()
    val tFoo = t.foo()
    if (tFoo !is X) {
        throw AssertionError("X expected: $tFoo")
    }

    return (t.foo() as X).x.toString() + t.bar().x.toString()
}