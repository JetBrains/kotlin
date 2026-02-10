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

class A(val x: Int, val y: Int)

inline operator fun A.component2(): Int = buzz(y)

// FILE: main.kt
import kotlin.test.*

// CHECK_NOT_CALLED: buzz
// CHECK_NOT_CALLED: component2

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

operator fun A.component1(): Int = fizz(x)

fun box(): String {
    val (a, b) = A(1, 2)
    assertEquals(a, 1)
    assertEquals(b, 2)
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}