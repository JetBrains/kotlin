// FILE: lib.kt
package foo
import kotlin.test.*

// CHECK_NOT_CALLED: buzz
inline fun <T> buzz(x: T): T {
    log("buzz($x)")
    return x
}

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

fun <T> assertArrayEquals(expected: Array<out T>, actual: Array<out T>, message: String? = null) {
    if (!arraysEqual(expected, actual)) {
        val msg = if (message == null) "" else ", message = '$message'"
        fail("Unexpected array: expected = '$expected', actual = '$actual'$msg")
    }
}

private fun <T> arraysEqual(first: Array<out T>, second: Array<out T>): Boolean {
    if (first === second) return true
    if (first.size != second.size) return false
    for (index in 0..first.size - 1) {
        if (!equal(first[index], second[index])) return false
    }
    return true
}

private fun equal(first: Any?, second: Any?) =
    if (first is Array<*> && second is Array<*>) {
        arraysEqual(first, second)
    }
    else {
        first == second
    }

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

fun box(): String {
    assertArrayEquals(arrayOf(1, 2), arrayOf(fizz(1), buzz(2)))
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}