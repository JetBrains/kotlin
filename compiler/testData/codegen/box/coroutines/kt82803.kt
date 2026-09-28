// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

suspend fun test() {
    try {
        xx(1)
    } catch (ex: Exception) {
        if (1 < 2) {
            if (false) {
                yy(ex.message)
                throw ex
            } else {
                return
            }
        } else {
            xx(2)
            throw ex
        }
    }
}

suspend fun xx(q: Int): XX {
    return XX(q)
}

class XX(val y: Int)

suspend fun yy(m: String?) {
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        test()
    }
    return "OK"
}
