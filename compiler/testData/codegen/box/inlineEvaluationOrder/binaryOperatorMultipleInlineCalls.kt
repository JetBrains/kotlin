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
    assertEquals(10, (fizz(1) + buzz(2)) + (fizz(3) + buzz(4)))
    assertEquals("fizz(1);buzz(2);fizz(3);buzz(4);", pullLog())

    return "OK"
}