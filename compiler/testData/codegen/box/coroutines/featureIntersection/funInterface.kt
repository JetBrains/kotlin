// WITH_RUNTIME
// WITH_COROUTINES
// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// LANGUAGE: +SuspendFunctionsInFunInterfaces, +JvmIrEnabledByDefault

import helpers.*
import kotlin.coroutines.*

fun interface Action {
    suspend fun run()
}
suspend fun runAction(a: Action) {
    a.run()
}
fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}
fun box(): String {
    var res = "FAIL"
    builder {
        runAction {
            res = "OK"
        }
    }
    return res
}