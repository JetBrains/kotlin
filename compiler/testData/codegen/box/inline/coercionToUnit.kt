// WITH_STDLIB

import kotlin.test.*

fun <T> myRun(action: () -> T): T = action()

fun foo(n: Number, b: Boolean) {
    n.let {
        if (b) return@let

        myRun() { 42 }
    }
}

fun box(): String {
    assertEquals(Unit, foo(42, false))
    return "OK"
}
