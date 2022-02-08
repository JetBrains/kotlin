// WITH_STDLIB

import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
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
    var res = "FAIL 1"
    builder {
        val getResult = GetResult()
        res = getResult().a as String
    }
    c?.resume(IC("OK"))
    if (res != "OK") return "FAIL 1 $res"

    res = "FAIL 2"
    builder {
        val getResult = GetResult()
        res = getResult.invoke().a as String
    }
    c?.resume(IC("OK"))
    if (res != "OK") return "FAIL 2 $res"

    res = "FAIL 3"
    builder {
        res = GetResult()().a as String
    }
    c?.resume(IC("OK"))
    if (res != "OK") return "FAIL 3 $res"

    res = "FAIL 4"
    builder {
        val getResult = IC1("OK")
        res = getResult().a as String
    }
    c?.resume(IC("OK"))
    if (res != "OK") return "FAIL 4 $res"

    res = "FAIL 5"
    builder {
        val getResult = IC1("OK")
        res = getResult.invoke().a as String
    }
    c?.resume(IC("OK"))
    if (res != "OK") return "FAIL 5 $res"

    res = "FAIL 6"
    builder {
        res = IC1("OK")().a as String
    }
    c?.resume(IC("OK"))
    if (res != "OK") return "FAIL 6 $res"
    return res
}
