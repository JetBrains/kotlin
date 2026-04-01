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

// CHECK_NOT_CALLED: buzz

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

class A {
    val x: Int

    init {
        x = fizz(1) + buzz(2)
    }
}

fun box(): String {
    val a = A()
    assertEquals(3, a.x)
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}