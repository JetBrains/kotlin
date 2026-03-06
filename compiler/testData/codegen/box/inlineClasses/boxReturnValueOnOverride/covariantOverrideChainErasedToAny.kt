// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: Any)

interface IBar {
    fun bar(): Any
}

interface IFoo : IBar {
    fun foo(): Any
    override fun bar(): X
}

class TestX : IFoo {
    override fun foo(): X = X("O")
    override fun bar(): X = X("K")
}

fun box(): String {
    val t: IFoo = TestX()
    val tFoo = t.foo()
    if (tFoo !is X) {
        throw AssertionError("X expected: $tFoo")
    }

    val t2: IBar = TestX()
    val tBar = t.bar()
    if (tBar !is X) {
        throw AssertionError("X expected: $tBar")
    }

    return (t.foo() as X).x!!.toString() + (t2.bar() as X).x!!.toString()
}