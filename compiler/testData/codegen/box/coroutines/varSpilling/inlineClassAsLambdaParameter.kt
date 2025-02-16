// WITH_STDLIB
// WITH_COROUTINES

// Because of @JvmInline
// TARGET_BACKEND: JVM
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    c = x
    COROUTINE_SUSPENDED
}

@JvmInline
value class IC(val s: String)

fun builder(c: suspend (ic: IC) -> Unit) {
    c.startCoroutine(IC("OK"), Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

var c: Continuation<Unit>? = null

fun box() : String {
    var res = "FAIL"
    builder { ic ->
        res = ic.s
        suspendHere()
        // No NPE here.
    }
    c?.resume(Unit)
    return res
}