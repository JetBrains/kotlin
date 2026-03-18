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

// FILE: main.kt
import kotlin.test.*

// CHECK_NOT_CALLED: buzz

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

fun box(): String {
    assertEquals(3, fizz(1) + buzz(2))
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}