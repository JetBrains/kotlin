// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class Controller {
    suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume("OK")
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = "fail 1"
    builder {
        // Initialize var with Int value
        for (i in 1..1) {
            if (i != 1) continue
        }

        // This variable should take the same slot as 'i' had
        var s: String

        // We should not spill 's' to continuation field because it's not initialized
        // More precisely it contains a value of wrong type (it conflicts with contents of local var table),
        // so an attempt of spilling may lead to problems on Android
        if (suspendHere() == "OK") {
            s = "OK"
        }
        else {
            s = "fail 2"
        }

        result = s
    }

    return result
}
