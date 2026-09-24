// TARGET_BACKEND: WASM
// WITH_STDLIB
// Heap pollution must be detected where a value produced at an erased generic type is narrowed back
// to a substituted type (an erasure boundary): with a ClassCastException, not a Wasm trap.
// Like suspendResult.kt, but the value arrives through `Continuation.resume` after a real suspension.
@file:Suppress("UNCHECKED_CAST")
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun runSuspend(block: suspend () -> Any?): String {
    var r = "FAIL: not completed"
    block.startCoroutine(Continuation(EmptyCoroutineContext) {
        r = when (val e = it.exceptionOrNull()) { null -> "FAIL: no exception"; is ClassCastException -> "OK"; else -> "FAIL: $e" }
    })
    return r
}

class Data(val x: Int)

fun expectCCE(block: () -> Any?): String =
    try { block(); "FAIL: no exception" } catch (e: ClassCastException) { "OK" } catch (e: Throwable) { "FAIL: $e" }

suspend fun <T> get(a: Any): T = suspendCoroutineUninterceptedOrReturn { c -> c.resume(a as T); COROUTINE_SUSPENDED }
fun box() = runSuspend { get<Data>("str").x }
