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

class Sum(x: Int, y: Int) {
    init {
        log("new Sum($x, $y)")
    }

    val value = x + y
}

fun box(): String {
    assertEquals(3, Sum(fizz(1), buzz(2)).value)
    assertEquals("fizz(1);buzz(2);new Sum(1, 2);", pullLog())

    return "OK"
}