// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var result = ""
}

suspend fun excs() { throw Exception("!!!") }
suspend fun fff() {}

suspend fun bars(): String {
    var i = 0
    var s = ""
    while (i < 3) {
        ++i
        s += "FAIL$i;"
        try {
            try {
                fff()
                return s
            } finally  {
                excs()
            }
        } catch (x: Exception) {
            continue
        }
    }

    return "OK"
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation)
    return controller.result
}

fun box(): String {
    return builder { result = bars() }
}
