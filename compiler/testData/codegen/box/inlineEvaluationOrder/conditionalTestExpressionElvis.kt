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

fun test(x: Boolean?): Boolean = buzz(x) ?: fizz(true)

fun box(): String {
    assertEquals(true, test(null))
    assertEquals("buzz(null);fizz(true);", pullLog())
    assertEquals(false, test(false))
    assertEquals("buzz(false);", pullLog())
    assertEquals(true, test(true))
    assertEquals("buzz(true);", pullLog())

    return "OK"
}