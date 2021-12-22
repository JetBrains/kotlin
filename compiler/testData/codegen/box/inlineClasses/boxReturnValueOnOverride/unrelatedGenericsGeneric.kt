// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IFoo1<out T> {
    fun foo(): T
}

interface IFoo2<out T> {
    fun foo(): T
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: String>(val x: T)

class Test : IFoo1<X<String>>, IFoo2<X<String>> {
    override fun foo(): X<String> = X("OK")
}

fun box(): String {
    val t1: IFoo1<Any> = Test()
    val foo1 = t1.foo()
    if (foo1 !is X<*>) {
        throw AssertionError("foo1 !is X: $foo1")
    }
    if (foo1.x != "OK") {
        throw AssertionError("foo1.x != 'OK': $foo1")
    }

    val t2: IFoo2<Any> = Test()
    val foo2 = t2.foo()
    if (foo2 !is X<*>) {
        throw AssertionError("foo2 !is X: $foo2")
    }
    if (foo2.x != "OK") {
        throw AssertionError("foo2.x != 'OK': $foo2")
    }

    return "OK"
}