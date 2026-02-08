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

fun test(x: Boolean?): Boolean = fizz(x) ?: buzz(true)

fun box(): String {
    assertEquals(true, test(null))
    assertEquals("fizz(null);buzz(true);", pullLog())
    assertEquals(false, test(false))
    assertEquals("fizz(false);", pullLog())
    assertEquals(true, test(true))
    assertEquals("fizz(true);", pullLog())

    return "OK"
}