// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IBase

interface IQ : IBase {
    fun ok(): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: IQ>(val t: T): IQ {
    override fun ok(): String = t.ok()
}

interface IFoo1 {
    fun foo(): Any
}

interface IFoo2<T : IBase> {
    fun foo(): T
}

object OK : IQ {
    override fun ok(): String = "OK"
}

class Test : IFoo1, IFoo2<IQ> {
    override fun foo(): X<IQ> = X(OK)
}

fun box(): String {
    val t1: IFoo1 = Test()
    val foo1 = t1.foo()
    if (foo1 !is IQ) {
        throw AssertionError("foo1 !is IQ: $foo1")
    }
    val ok1 = foo1.ok()
    if (ok1 != "OK") {
        throw AssertionError("ok1: $ok1")
    }
    if (foo1 !is X<*>) {
        throw AssertionError("foo1 !is X: $foo1")
    }

    val t2: IFoo2<IQ> = Test()
    val foo2 = t2.foo()
    if (foo2 !is X<*>) {
        throw AssertionError("foo2 !is X: $foo2")
    }
    val ok2 = foo2.ok()
    if (ok2 != "OK") {
        throw AssertionError("ok1: $ok2")
    }

    return "OK"
}