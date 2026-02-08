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

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

fun box(): String {
    assertEquals(false, fizz(false) && buzz(false))
    assertEquals("fizz(false);", pullLog())

    assertEquals(false, fizz(true) && buzz(false))
    assertEquals("fizz(true);buzz(false);", pullLog())

    assertEquals(true, fizz(true) && buzz(true))
    assertEquals("fizz(true);buzz(true);", pullLog())

    return "OK"
}