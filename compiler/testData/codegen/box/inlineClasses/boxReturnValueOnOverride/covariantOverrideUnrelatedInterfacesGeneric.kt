// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IQ1
interface IQ2

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: Any>(val x: T): IQ1, IQ2

interface IFoo1 {
    fun foo(): IQ1
}

interface IFoo2 {
    fun foo(): IQ2
}

class Test : IFoo1, IFoo2 {
    override fun foo() = X("OK")
}

fun box(): String {
    val t1: IFoo1 = Test()
    val x1 = t1.foo()
    if (x1 !is X<*>) {
        throw AssertionError("x1: X expected: $x1")
    }
    if (x1.x != "OK") {
        throw AssertionError("x1: ${x1.x}")
    }

    val t2: IFoo2 = Test()
    val x2 = t2.foo()
    if (x2 !is X<*>) {
        throw AssertionError("x2: X expected: $x2")
    }
    if (x2.x != "OK") {
        throw AssertionError("x2: ${x2.x}")
    }

    return "OK"
}