// !LANGUAGE: +SuspendFunctionsInFunInterfaces +JvmIrEnabledByDefault
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

var c: Continuation<Unit>? = null

suspend fun suspendMe() = suspendCoroutine<Unit> { continuation ->
    c = continuation
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

fun interface SuspendFoo {
    suspend fun foo()
}

fun box(): String {
    var test = ""
    val lambda = SuspendFoo {
        suspendMe()
        test += "O"
        suspendMe()
        test += "K"
    }
    builder {
        lambda.foo()
    }
    c?.resume(Unit)
    c?.resume(Unit)
    return test
}
