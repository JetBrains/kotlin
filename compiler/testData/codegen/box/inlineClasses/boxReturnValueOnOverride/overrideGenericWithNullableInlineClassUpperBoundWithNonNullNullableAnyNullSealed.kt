// IGNORE_BACKEND: JS_IR
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR_ES6
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: Any?): IC()

interface IFoo<out T : IC?> {
    fun foo(): T
}

class Test : IFoo<IC> {
    override fun foo(): IC = X(null)
}

fun box(): String {
    val t1: IFoo<IC?> = Test()
    val x1 = t1.foo()
    if (x1 != X(null)) throw AssertionError("x1: $x1")

    val t2 = Test()
    val x2 = t2.foo()
    if (x2 != X(null)) throw AssertionError("x2: $x2")

    return "OK"
}