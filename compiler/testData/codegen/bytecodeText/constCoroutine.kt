// LANGUAGE_VERSION: 1.2
// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
// TREAT_AS_ONE_FILE
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*
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

// 2 GETSTATIC kotlin/Unit.INSTANCE
// 1 GETSTATIC helpers/EmptyContinuation.Companion
// 3 GETSTATIC
