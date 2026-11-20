// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-JS:2.4
// KT-90564: Fixed in 2.5.20-Beta1
// WITH_STDLIB
// WITH_COROUTINES
// FILE: another.kt

private val prop = run {
    log("prop")
}

suspend fun foo(unused: Unit = boo()) {
    log("foo")
}

// FILE: util.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

fun boo() {
    log("boo")
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    log("S")
    suspend { foo() }.startCoroutine(EmptyContinuation)
    log("E")

    if (l != "S;prop;boo;foo;E;") return l
    return "OK"
}
