// ISSUE: KT-91167
// WITH_STDLIB
// WITH_COROUTINES
// FILE: log.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

suspend fun suspendLog(a: String) {
    log(a)
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    log("S")
    val t = Foo()
    log("M")
    var result: String? = null
    t.action.startCoroutine(handleResultContinuation { result = it })
    log("E")

    if (result != "prop value") return "Fail result: $result"
    if (l != "S;Foo init;M;Foo lambda;top-level prop;after read;E;") return l
    return "OK"
}

// FILE: another.kt

private val prop = run {
    log("top-level prop")
    "prop value"
}

class Foo {
    init {
        log("Foo init")
    }
    val action: suspend () -> String = {
        suspendLog("Foo lambda")
        val value = prop
        suspendLog("after read")
        value
    }
}
