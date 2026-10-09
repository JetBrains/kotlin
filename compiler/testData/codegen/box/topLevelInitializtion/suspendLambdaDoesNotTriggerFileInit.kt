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
    t.action.startCoroutine(EmptyContinuation)
    log("E")

    if (l != "S;Foo init;M;Foo lambda;E;") return l
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
    val action: suspend () -> Unit = {
        log("Foo lambda")
    }
}
