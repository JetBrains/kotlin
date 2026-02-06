// WITH_COROUTINES

// FILE: main.kt

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

external fun externalLog(vararg params: Any?): Array<String>

external fun externalLog(
    arg: Int = definedExternally,
    vararg args: String,
    o: Long
) : Array<String>

suspend fun test1(p: String): String {
    return "A" + p
}

suspend fun test2(p: String): String {
    return p
}

suspend fun foo(): Array<String> {
    return externalLog(test1("B"))
}

suspend fun bar(): Array<String> {
    return externalLog(test2("C"), "D", test2("E"))
}

suspend fun qux(): Array<String> {
    return externalLog(test2("G"), test2("F"))
}

suspend fun test3(s1: String, s2: String): Array<Any?> {
    return arrayOf(s1, "Y", s2)
}

suspend fun tef(): Array<String> {
    return externalLog("H", *test3("I", "J"), "K")
}

suspend fun bux(): Array<String> {
    return externalLog(*test3("L", "M"))
}

suspend fun foo2(): Array<String> {
    return externalLog(1, test1("B"), 1000L)
}

suspend fun bar2(): Array<String> {
    return externalLog(1, test2("C"), "D", test2("E"), 2000L)
}

suspend fun qux2(): Array<String> {
    return externalLog(test2("G"), test2("F"), 3000L)
}

suspend fun tef2(): Array<String> {
    return externalLog(4, "H", *test3("I", "J"), "K", 5000L)
}

suspend fun bux2(): Array<String> {
    return externalLog(*test3("L", "M"), 6000L)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result += foo()[0]
        val b = bar()
        result += b[0]
        result += b[1]
        result += b[2]
        val q = qux()
        result += q[0]
        result += q[1]
        val t = tef()
        result += t[0]
        result += t[1]
        result += t[2]
        result += t[3]
        result += t[4]
        val bx = bux()
        result += bx[0]
        result += bx[1]
        result += bx[2]
    }

    if (result != "ABCDEGFHIYJKLYM") return "FAIL1: $result"

    result = ""

    builder {
        val f = foo2()
        result += f[0]
        result += f[1]
        result += f[2]
        val b = bar2()
        result += b[0]
        result += b[1]
        result += b[2]
        result += b[3]
        result += b[4]
        val q = qux2()
        result += q[0]
        result += q[1]
        result += q[2]
        val t = tef2()
        result += t[0]
        result += t[1]
        result += t[2]
        result += t[3]
        result += t[4]
        result += t[5]
        result += t[6]
        val bx = bux2()
        result += bx[0]
        result += bx[1]
        result += bx[2]
        result += bx[3]
    }

    if (result != "1AB10001CDE2000GF30004HIYJK5000LYM6000") return "FAIL2: $result"

    return "OK"
}

// FILE: main.js

function externalLog() {
    return arguments
}