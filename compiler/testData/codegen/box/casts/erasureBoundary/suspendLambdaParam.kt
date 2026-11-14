// TARGET_BACKEND: WASM
// WITH_STDLIB
// Heap pollution must be detected where a value produced at an erased generic type is narrowed back
// to a substituted type (an erasure boundary): with a ClassCastException, not a Wasm trap.
// A suspend lambda narrows its erased parameter when called through `SuspendFunction1.invoke`.
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

suspend fun <T> call(a: Any, block: suspend (T) -> Int): Int = block(a as T)
fun box() = runSuspend { call<Data>("str") { it.x } }
