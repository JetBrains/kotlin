// FILE: lib.kt
package foo

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

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

fun test(x: Boolean): Boolean =
        if (buzz(x)) buzz(true) else fizz(false)

fun box(): String {
    assertEquals(true, test(true))
    assertEquals("buzz(true);buzz(true);", pullLog())
    assertEquals(false, test(false))
    assertEquals("buzz(false);fizz(false);", pullLog())

    return "OK"
}