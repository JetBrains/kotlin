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

fun test(x: Boolean, y: Boolean): Boolean {
    if (fizz(x) && buzz(y)) {
        return true
    }

    return false
}


fun box(): String {
    assertEquals(false, test(false, true))
    assertEquals("fizz(false);", pullLog())

    assertEquals(false, test(true, false))
    assertEquals("fizz(true);buzz(false);", pullLog())

    assertEquals(true, test(true, true))
    assertEquals("fizz(true);buzz(true);", pullLog())

    return "OK"
}