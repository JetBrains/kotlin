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
