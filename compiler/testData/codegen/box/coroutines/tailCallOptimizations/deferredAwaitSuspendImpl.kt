// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// JVM_ABI_K1_K2_DIFF: KT-63864

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun awaitInternal(): Any? = TailCallOptimizationChecker.saveStackTrace()

interface Deferred<out T> {
    suspend fun await(): T
}

open class DeferredCoroutine<T> : Deferred<T> {
    override suspend fun await(): T = awaitInternal() as T
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        DeferredCoroutine<String>().await()
    }
    TailCallOptimizationChecker.checkNoStateMachineIn("await\$suspendImpl")
    return "OK"
}