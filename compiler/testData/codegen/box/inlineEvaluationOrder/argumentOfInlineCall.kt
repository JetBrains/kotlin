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

// CHECK_NOT_CALLED_IN_SCOPE: function=max scope=box

inline fun max(a: Int, b: Int): Int {
    log("max($a, $b)")

    if (a > b) return a

    return b
}

fun box(): String {
    val test = max(fizz(1), max(fizz(2), buzz(3)))
    assertEquals(3, test)
    assertEquals("fizz(1);fizz(2);buzz(3);max(2, 3);max(1, 3);", pullLog())

    return "OK"
}