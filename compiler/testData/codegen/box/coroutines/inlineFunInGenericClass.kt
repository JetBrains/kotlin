// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendThere(v: Any?): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v?.toString() ?: "<empty>")
    COROUTINE_SUSPENDED
}


class A<T>(val arg: T) {
    var result = ""
    inline suspend fun foo() {
        result = suspendThere(arg)
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    val a = A<String>("OK")
    builder {
        a.foo()
    }

    return a.result
}
