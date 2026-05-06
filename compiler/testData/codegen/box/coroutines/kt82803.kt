// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.0,2.1,2.2,2.3
// ^^^ KT-82803, KT-83728 are fixed in 2.4.0-Beta1

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
