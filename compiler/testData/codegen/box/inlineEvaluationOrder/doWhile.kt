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

fun box(): String {
    var c = 2

    do {
        if (c > 4) throw Exception("Timeout!")
        c++
    } while (buzz(c) < 4)

    assertEquals("buzz(3);buzz(4);", pullLog())

    return "OK"
}