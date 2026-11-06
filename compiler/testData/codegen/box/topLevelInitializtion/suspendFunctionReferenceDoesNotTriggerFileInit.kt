// ISSUE: KT-91167
// WITH_STDLIB
// WITH_COROUTINES
// FILE: log.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    log("S")
    val t = Foo()
    log("M")
    t.reference.startCoroutine(EmptyContinuation)
    log("E")

    if (l != "S;Foo init;M;target;E;") return l
    return "OK"
}

// FILE: another.kt

private val prop = run {
    log("top-level prop")
}

class Foo {
    init {
        log("Foo init")
    }
    val reference: suspend () -> Unit = ::target
}

// FILE: target.kt

suspend fun target() {
    log("target")
}
