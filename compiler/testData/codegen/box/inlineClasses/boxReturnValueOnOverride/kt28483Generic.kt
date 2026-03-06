// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class ResultOrClosed<T>(val x: T)

interface A<T> {
    fun foo(): T
}

class B : A<ResultOrClosed<String>> {
    override fun foo(): ResultOrClosed<String> = ResultOrClosed("OK")
}

fun box(): String {
    val foo: Any = (B() as A<ResultOrClosed<String>>).foo()
    if (foo !is ResultOrClosed<*>) throw AssertionError("foo: $foo")
    return foo.x.toString()
}