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

fun multiplyFun(): (Int, Int)->Int {
    log("multiplyFun()")
    return { x, y -> x * y }
}

fun box(): String {
    assertEquals(6, multiplyFun()(fizz(2), buzz(3)))
    assertEquals("multiplyFun();fizz(2);buzz(3);", pullLog())

    return "OK"
}