// WITH_RUNTIME
// WITH_COROUTINES
// SKIP_MANGLE_VERIFICATION
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendThenThrow(): Nothing {
    suspendCoroutineUninterceptedOrReturn<Unit> {
        it.resume(Unit)
        COROUTINE_SUSPENDED
    }
    throw RuntimeException()
}

suspend fun foo(x: Int = 1): Nothing = suspendThenThrow()

interface I {
    suspend fun bar(): Nothing
}

class C : I by (object : I {
    override suspend fun bar(): Nothing = suspendThenThrow()
})

var result = ""

fun box(): String {
    suspend {
        try { foo() } catch (e: RuntimeException) { result += "O" }
        try { C().bar() } catch (e: RuntimeException) { result += "K" }
    }.startCoroutine(EmptyContinuation)
    return result
}
