// FILE: lib.kt
package foo
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

// FILE: main.kt
package foo
import kotlin.test.*

// CHECK_NOT_CALLED: buzz

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

// Test for KT-7502

class A(val value: Int) {
    fun plus(num: Int): Int = this.value + num
}

fun box(): String {
    assertEquals(15, A(fizz(5)).plus(buzz(10)))
    assertEquals("fizz(5);buzz(10);", pullLog())

    return "OK"
}