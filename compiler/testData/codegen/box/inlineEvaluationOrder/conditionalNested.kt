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

fun test(x: Boolean, y: Boolean): Int =
        if (fizz(x))
            if (fizz(y)) buzz(1) else buzz(2)
        else
            if (fizz(y)) buzz(3) else buzz(4)

fun box(): String {
    assertEquals(1, test(true, true))
    assertEquals("fizz(true);fizz(true);buzz(1);", pullLog())

    assertEquals(2, test(true, false))
    assertEquals("fizz(true);fizz(false);buzz(2);", pullLog())

    assertEquals(3, test(false, true))
    assertEquals("fizz(false);fizz(true);buzz(3);", pullLog())

    assertEquals(4, test(false, false))
    assertEquals("fizz(false);fizz(false);buzz(4);", pullLog())

    return "OK"
}