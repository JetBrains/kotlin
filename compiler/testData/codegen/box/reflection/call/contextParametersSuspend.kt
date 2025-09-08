// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// WITH_REFLECT
// WITH_COROUTINES


import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.reflect.KCallable
import kotlin.reflect.full.callSuspend

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class TestClass {
    suspend context(a: suspend (suspend () -> String) -> String) fun superSuspendWithSuspendContextLambda(b: suspend () -> String): String {
        suspendHere()
        return a.invoke(b)
    }
}

fun box(): String {
    var result = ""

    val f = TestClass::class.members.single { it.name == "superSuspendWithSuspendContextLambda" } as KCallable<String>
    val contextParam: SuspendFunction1<SuspendFunction0<String>, String> = { a: suspend () -> String ->
        suspendHere()
        a.invoke()
    }

    builder {
        f.callSuspend(
            TestClass(),
            contextParam,
            suspend {
                suspendHere()
                result += "OK"
            }
        )
    }

    return result
}
