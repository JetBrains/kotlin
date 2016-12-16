// WITH_RUNTIME
// WITH_COROUTINES
// TREAT_AS_ONE_FILE
import kotlin.coroutines.*
suspend fun suspendHere() = ""

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {

    for (i in 1..3) {
        builder {
            if (suspendHere() != "OK") throw java.lang.RuntimeException("fail 1")
        }
    }

    return "OK"
}

// 1 GETSTATIC kotlin/Unit.INSTANCE : Lkotlin/Unit;
// 1 GETSTATIC EmptyContinuation.INSTANCE
// 1 GETSTATIC kotlin/coroutines/CoroutineIntrinsics.INSTANCE
// 3 GETSTATIC
