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

class A(var x: Int) {
    override fun toString(): String = "A($x)"
}

fun box(): String {
    val a = A(10)
    fizz(a).x = buzz(20)
    assertEquals(20, a.x)
    assertEquals("fizz(A(10));buzz(20);", pullLog())

    return "OK"
}