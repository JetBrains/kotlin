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
