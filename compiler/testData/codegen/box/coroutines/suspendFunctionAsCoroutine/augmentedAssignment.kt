// IGNORE_BACKEND: JVM
// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*

// TODO: looks like this is a bug in JVM backend
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendThere(v: A): A = suspendCoroutineOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

class A(val value: String) {
    operator suspend fun plus(other: A) = suspendThere(A(value + other.value))
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}


fun box(): String {
    var a = A("O")

    builder {
        a += A("K")
    }

    return a.value
}
