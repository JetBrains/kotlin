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
    assertEquals(2, arrayOf(1, 2)[fizz(0) + buzz(1)])
    assertEquals("fizz(0);buzz(1);", pullLog())

    return "OK"
}