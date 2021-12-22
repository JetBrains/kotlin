// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: Any>(val x: T)

interface IFoo {
    fun foo(): X<String>?
}

class Test : IFoo {
    override fun foo(): X<String>? = X("OK")
}

fun box(): String {
    val t1: IFoo = Test()
    val x1 = t1.foo()
    if (x1 != X("OK")) throw AssertionError("x1: $x1")

    val t2 = Test()
    val x2 = t2.foo()
    if (x2 != X("OK")) throw AssertionError("x2: $x2")

    return "OK"
}