// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var result = ""
    var ok = false
    suspend fun suspendHere(v: String): Unit = suspendCoroutineUninterceptedOrReturn { x ->
        result += v
        x.resume(Unit)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, handleResultContinuation {
        controller.ok = true
    })
    if (!controller.ok) throw RuntimeException("Fail ok")
    return controller.result
}

fun box(): String {
    val r1 = builder {
        for (i in 5..6) {
            suspendHere(i.toString())
        }
    }

    if (r1 != "56") return "fail 1: $r1"

    val r2 = builder {
        var i = 7
        while (i <= 8) {
            suspendHere(i.toString())
            i++
        }
    }

    if (r2 != "78") return "fail 2: $r2"

    val r3 = builder {
        var i = 9
        do {
            suspendHere(i.toString())
            i++
        } while (i <= 10);
    }

    if (r3 != "910") return "fail 3: $r3"

    return "OK"
}
