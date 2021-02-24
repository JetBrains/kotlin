// WITH_RUNTIME
// WITH_COROUTINES
// TARGET_BACKEND: JVM

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleResultContinuation {
        proceed = null
    })
}

suspend fun foo(until: Int): String {
    val o = "O"
    val k = "K"
    val dot = "."
    suspend fun bar(x: Int): String =
        if (x == until) dot else if (x < until) o + bar(x * 2) else k + bar(x - 1)
    return bar(1)
}

var proceed: (() -> Unit)? = null

suspend fun suspendHere(value: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    proceed = {
        x.resume(value)
    }
    COROUTINE_SUSPENDED
}

suspend fun foo2(until: Int): String {
    val o = "O"
    val k = "K"
    val dot = "."
    suspend fun bar(x: Int): String =
        if (x == until) dot else if (x < until) suspendHere(o + bar(x * 2)) else suspendHere(k + bar(x - 1))
    return bar(1)
}

suspend fun fooCallableReference(until: Int): String {
    val o = "O"
    val k = "K"
    val dot = "."
    suspend fun bar(x: Int): String =
        if (x == until) dot else if (x < until) o + (::bar)(x * 2) else k + (::bar)(x - 1)
    return bar(1)
}

suspend fun fooCallableReferenceIndirectRecursion(until: Int): String {
    val o = "O"
    val k = "K"
    val dot = "."
    suspend fun bar(x: Int): String {
        suspend fun innerO() = o + (::bar)(x * 2)
        suspend fun innerK() = k + (::bar)(x - 1)
        return if (x == until) dot else if (x < until) innerO() else innerK()
    }
    return bar(1)
}

fun box(): String {
    var res = "FAIL 1"
    builder {
        res = foo(10)
    }
    if (res != "OOOOKKKKKK.") return "FAIL 1: $res"
    res = "FAIL 2"
    builder {
        res = foo2(10)
    }
    while (proceed != null) {
        proceed!!()
    }
    if (res != "OOOOKKKKKK.") return "FAIL 2: $res"
    res = "FAIL 3"
    builder {
        res = fooCallableReference(10)
    }
    if (res != "OOOOKKKKKK.") return "FAIL 3: $res"
    res = "FAIL 4"
    builder {
        res = fooCallableReferenceIndirectRecursion(10)
    }
    if (res != "OOOOKKKKKK.") return "FAIL 4: $res"
    return "OK"
}
