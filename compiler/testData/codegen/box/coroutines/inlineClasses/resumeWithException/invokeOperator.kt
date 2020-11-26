// WITH_RUNTIME
// WITH_COROUTINES

import kotlin.coroutines.*
import helpers.*

var result = "FAIL"

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleExceptionContinuation {
        result = it.message!!
    })
}

inline class IC(val a: Any?)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

class GetResult {
    suspend operator fun invoke(): IC = suspendMe()
}

inline class IC1(val a: String) {
    suspend operator fun invoke(): IC = suspendMe()
}

fun box(): String {
    builder {
        val getResult = GetResult()
        getResult()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL 2"
    builder {
        val getResult = GetResult()
        getResult.invoke()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 2 $result"

    result = "FAIL 3"
    builder {
        GetResult()()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 3 $result"

    result = "FAIL 4"
    builder {
        val getResult = IC1("OK")
        getResult()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 4 $result"

    result = "FAIL 5"
    builder {
        val getResult = IC1("OK")
        getResult.invoke()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 5 $result"

    result = "FAIL 6"
    builder {
        IC1("OK")()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 6 $result"
    return result
}
