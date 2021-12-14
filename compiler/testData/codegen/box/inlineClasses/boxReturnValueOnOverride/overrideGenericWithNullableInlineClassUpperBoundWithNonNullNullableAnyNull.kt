// IGNORE_BACKEND: JS_IR
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR_ES6
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: Any?)

interface IFoo<out T : X?> {
    fun foo(): T
}

class Test : IFoo<X> {
    override fun foo(): X = X(null)
}

fun box(): String {
    val t1: IFoo<X?> = Test()
    val x1 = t1.foo()
    if (x1 != X(null)) throw AssertionError("x1: $x1")

    val t2 = Test()
    val x2 = t2.foo()
    if (x2 != X(null)) throw AssertionError("x2: $x2")

    return "OK"
}