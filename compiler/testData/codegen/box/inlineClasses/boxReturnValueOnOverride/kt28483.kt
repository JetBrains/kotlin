// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
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