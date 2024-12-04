// WITH_RUNTIME

class A {
    fun foo(x: Int = 32) = "OK"
}

var result = "failed"

fun whoops(x: Any) {
    when (x) {
        is A -> result = x.foo()
        else -> throw AssertionError()
    }
}

fun box(): String {
    whoops(A())
    return result
}
