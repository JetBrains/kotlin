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

fun sum(x: Int, y: Int): Int {
    log("sum($x, $y)")
    return x + y
}

fun box(): String {
    assertEquals(3, sum(fizz(1), buzz(2)))
    assertEquals("fizz(1);buzz(2);sum(1, 2);", pullLog())

    return "OK"
}