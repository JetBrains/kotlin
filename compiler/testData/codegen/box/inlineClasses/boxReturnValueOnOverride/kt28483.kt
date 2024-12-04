// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class ResultOrClosed(val x: Any?)

interface A<T> {
    fun foo(): T
}

class B : A<ResultOrClosed> {
    override fun foo(): ResultOrClosed = ResultOrClosed("OK")
}

fun box(): String {
    val foo: Any = (B() as A<ResultOrClosed>).foo()
    if (foo !is ResultOrClosed) throw AssertionError("foo: $foo")
    return foo.x.toString()
}