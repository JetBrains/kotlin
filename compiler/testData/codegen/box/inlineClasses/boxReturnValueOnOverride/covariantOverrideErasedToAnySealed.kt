// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: Any): IC()

interface IFoo {
    fun foo(): Any
    fun bar(): IC
}

class TestX : IFoo {
    override fun foo(): IC = X("O")
    override fun bar(): IC = X("K")
}

fun box(): String {
    val t: IFoo = TestX()
    val tFoo = t.foo()
    if (tFoo !is X) {
        throw AssertionError("X expected: $tFoo")
    }

    return (t.foo() as X).x.toString() + (t.bar() as X).x.toString()
}