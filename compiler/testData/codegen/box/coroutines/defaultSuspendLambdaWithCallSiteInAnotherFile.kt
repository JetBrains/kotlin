// WITH_STDLIB
// WITH_COROUTINES
// ISSUE: KT-73462

// FILE: a.kt
import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        foo {}
        foo { result += "foobar;" }
    }
    if (result != "foo;foo;foobar;") return "fail: $result"
    return "OK"
}

// FILE: b.kt
var result = ""

suspend fun bar() {
    result += "bar;"
}

suspend fun foo(
    arg: (suspend () -> Unit) = { bar() },
) {
    result += "foo;"
    arg()
}
