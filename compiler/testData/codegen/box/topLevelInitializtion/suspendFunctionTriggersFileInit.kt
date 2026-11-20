// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-JS:2.4
// KT-90564: Fixed in 2.5.20-Beta1
// ISSUE: KT-90564
// WITH_STDLIB
// WITH_COROUTINES

// FILE: another.kt

private val prop = run {
    log("prop")
}

fun regular() {
    log("regular")
}

suspend fun susp() {
    log("susp")
}

// FILE: util.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    log("S")
    suspend { susp() }.startCoroutine(EmptyContinuation)
    regular()
    log("E")

    if (l != "S;prop;susp;regular;E;") return l
    return "OK"
}
