// ISSUE: KT-91167
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-JS:2.4
// ^^^ KT-91167 is fixed in 2.5.20
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

    if (l != "S;Foo init;M;target prop;target;E;") return l
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

private val targetProp = run {
    log("target prop")
}

suspend fun target() {
    log("target")
}
