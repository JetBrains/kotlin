// TARGET_BACKEND: WASM
// WITH_STDLIB
// Heap pollution must be detected where a value produced at an erased generic type is narrowed back
// to a substituted type (an erasure boundary): with a ClassCastException, not a Wasm trap.
// The adapter of a suspend function reference narrows its erased `invoke` parameter.
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

suspend fun useData(d: Data) = d.x
fun box() = runSuspend { val g: suspend (Data) -> Int = ::useData; (g as suspend (Any) -> Int)("str") }
