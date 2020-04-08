// IGNORE_BACKEND_FIR: JVM_IR

inline class ResultOrClosed(val x: Any?)

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