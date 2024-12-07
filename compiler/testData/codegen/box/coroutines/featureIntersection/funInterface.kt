// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JVM
// LANGUAGE: +SuspendFunctionsInFunInterfaces, +JvmIrEnabledByDefault
// SKIP_DCE_DRIVEN

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
