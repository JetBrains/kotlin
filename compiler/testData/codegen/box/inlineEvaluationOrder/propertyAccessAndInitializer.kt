// FILE: lib.kt
package foo

private var LOG = ""

fun log(string: String) {
    LOG += "$string;"
}

fun pullLog(): String {
    val string = LOG
    LOG = ""
    return string
}

inline fun bar(value: Int) {
    log("bar->begin")
    log("value=$value")
    log("bar->end")
}

// FILE: main.kt
package foo
import kotlin.test.*

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

object A {
    init {
        log("A.init")
    }

    val x = 23
}

fun box(): String {
    bar(A.x)
    assertEquals("A.init;bar->begin;value=23;bar->end;", pullLog())
    return "OK"
}