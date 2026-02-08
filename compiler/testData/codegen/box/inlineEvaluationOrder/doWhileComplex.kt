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
    var c = 1

    do {
        if (c > 4) throw Exception("Timeout!")
        c++
    } while (fizz(c) % 2 == 0 || buzz(c) % 3 == 0)

    assertEquals("fizz(2);fizz(3);buzz(3);fizz(4);fizz(5);buzz(5);", pullLog())

    return "OK"
}