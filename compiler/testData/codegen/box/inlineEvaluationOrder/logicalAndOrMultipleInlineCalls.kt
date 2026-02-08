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

fun box(): String {
    assertEquals(true, fizz(true) || buzz(true) && (fizz(false) || buzz(true)))
    assertEquals("fizz(true);", pullLog())

    return "OK"
}