// ISSUE: KT-7502
// FILE: lib.kt
import kotlin.test.*

inline fun <T> buzz(x: T): T {
    log("buzz($x)")
    return x
}

// CHECK_NOT_CALLED: buzz

private var LOG = ""

fun log(string: String) {
    LOG += "$string;"
}

fun pullLog(): String {
    val string = LOG
    LOG = ""
    return string
}

class A(val value: Int) {
    inline fun plus(num: Int): Int = this.value + num
}

// FILE: main.kt
import kotlin.test.*

// CHECK_NOT_CALLED: buzz
// CHECK_NOT_CALLED: plus

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

fun box(): String {
    assertEquals(15, A(fizz(5)).plus(buzz(10)))
    assertEquals("fizz(5);buzz(10);", pullLog())

    return "OK"
}