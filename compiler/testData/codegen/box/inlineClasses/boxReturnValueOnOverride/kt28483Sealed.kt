// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class ResultOrClosed(val x: Any?): IC()

interface A<T> {
    fun foo(): T
}

class B : A<IC> {
    override fun foo(): IC = ResultOrClosed("OK")
}

fun box(): String {
    val foo: Any = (B() as A<IC>).foo()
    if (foo !is ResultOrClosed) throw AssertionError("foo: $foo")
    return foo.x.toString()
}