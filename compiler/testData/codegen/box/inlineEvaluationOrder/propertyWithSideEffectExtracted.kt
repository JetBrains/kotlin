// Looks similar to KT-7674
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

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

inline fun bar(): Int {
    log("bar")
    return 10
}

val x: Int
    get() {
        log("x")
        return 1
    }

fun box(): String {
    assertEquals(12, x + bar() + x)
    assertEquals("x;bar;x;", pullLog())

    return "OK"
}