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

fun test(x: Boolean): Boolean =
        if (fizz(x)) buzz(true) else buzz(false)

fun box(): String {
    assertEquals(true, test(true))
    assertEquals("fizz(true);buzz(true);", pullLog())
    assertEquals(false, test(false))
    assertEquals("fizz(false);buzz(false);", pullLog())

    return "OK"
}