// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface GFoo<out T> {
    fun foo(): T
}

interface IBar {
    fun bar(): String
}

interface SFooBar : GFoo<IBar>

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: String>(val x: T) : IBar {
    override fun bar(): String = x
}

class Test : SFooBar {
    override fun foo() = X("OK")
}

fun box(): String {
    val t1: SFooBar = Test()
    val foo1 = t1.foo()
    if (foo1 !is X<*>) {
        throw AssertionError("foo1: $foo1")
    }
    val bar1 = foo1.bar()
    if (bar1 != "OK") {
        throw AssertionError("bar1: $bar1")
    }

    val t2: GFoo<Any> = Test()
    val foo2 = t2.foo()
    if (foo2 !is IBar) {
        throw AssertionError("foo2 !is IBar: $foo2")
    }
    val bar2 = foo2.bar()
    if (bar2 != "OK") {
        throw AssertionError("bar2: $bar2")
    }
    if (foo2 !is X<*>) {
        throw AssertionError("foo2 !is X: $foo2")
    }

    return "OK"
}