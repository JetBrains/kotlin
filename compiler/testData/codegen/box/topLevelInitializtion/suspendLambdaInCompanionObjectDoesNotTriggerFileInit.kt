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
    val action = Foo.action
    log("M")
    action.startCoroutine(EmptyContinuation)
    log("E")

    if (l != "S;Companion init;M;lambda start;lambda end;E;") return l
    return "OK"
}

// FILE: another.kt

private val prop = run {
    log("top-level prop")
}

class Foo {
    companion object {
        init {
            log("Companion init")
        }
        val action: suspend () -> Unit = {
            suspendLog("lambda start")
            suspendLog("lambda end")
        }
    }
}
