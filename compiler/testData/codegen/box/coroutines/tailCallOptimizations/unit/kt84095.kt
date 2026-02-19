// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun consume(value: Any) {}

class MyClass<T1> {
    // ambiguity of the type parameter names (for class and the function) is intentional here
    suspend inline fun <T1> suspendCancellableCoroutine(
        crossinline block: (Continuation<T1>) -> Unit
    ): T1 {
        return suspendCoroutineUninterceptedOrReturn { uCont: Continuation<T1> ->
            TailCallOptimizationChecker.saveStackTrace(uCont)
            block(uCont)
            Unit as T1
        }
    }

    suspend inline fun <T> withTwoInlineLayers(
        p1: Int,
        crossinline block: (Continuation<T>) -> Unit
    ): T {
        return suspendCancellableCoroutine(block)
    }

    suspend inline fun <T1> withExplicitUnitReturnType(
        crossinline block: (Continuation<T1>) -> Unit
    ): Unit {
        suspendCancellableCoroutine(block)
    }

    suspend inline fun <T1, T2> withTwoTypeParameters(
        crossinline block: (Continuation<T1>) -> Unit
    ): T1 {
        return suspendCancellableCoroutine(block)
    }

    suspend inline fun <T> withDifferentGenericReturnType(
        crossinline block: (Continuation<T1>) -> Unit
    ): T1 {
        return suspendCoroutineUninterceptedOrReturn { uCont: Continuation<T1> ->
            TailCallOptimizationChecker.saveStackTrace(uCont)
            block(uCont)
            Unit as T1
        }
    }
}

suspend fun delay(timeMillis: Long) {
    if (timeMillis <= 0) return
    return MyClass<String>().suspendCancellableCoroutine { cont: Continuation<Unit> ->
        consume(timeMillis)
    }
}

suspend fun delay2(timeMillis: Long) {
    if (timeMillis <= 0) return
    return MyClass<String>().withTwoInlineLayers(2) { cont: Continuation<Unit> ->
        consume(timeMillis)
    }
}

suspend fun delay3(timeMillis: Long) {
    if (timeMillis <= 0) return
    return MyClass<String>().withExplicitUnitReturnType { cont: Continuation<Unit> ->
        consume(timeMillis)
    }
}

suspend fun delay4_unsupported(timeMillis: Long) {
    if (timeMillis <= 0) return
    return MyClass<String>().withTwoTypeParameters<Unit,Unit> { cont: Continuation<Unit> ->
        consume(timeMillis)
    }
}

suspend fun delay5_unsupported(timeMillis: Long) {
    if (timeMillis <= 0) return
    MyClass<Unit>().withDifferentGenericReturnType<Unit> {
        consume(timeMillis)
    }
}


fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        delay(1000)
        TailCallOptimizationChecker.checkNoStateMachineIn("delay")

        delay2(1000)
        TailCallOptimizationChecker.checkNoStateMachineIn("delay2")

        delay3(1000)
        TailCallOptimizationChecker.checkNoStateMachineIn("delay3")

        delay4_unsupported(1000)
        TailCallOptimizationChecker.checkStateMachineIn("delay4_unsupported")

        delay5_unsupported(1000)
        TailCallOptimizationChecker.checkStateMachineIn("delay5_unsupported")
    }
    return "OK"
}