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

fun sum(a: Int, b: Int, c: Int, d: Int): Int {
    log("sum($a, $b, $c, $d)")
    return a + b + c + d
}

fun box(): String {
    assertEquals(10, sum(fizz(1), buzz(2), fizz(3), buzz(4)))
    assertEquals("fizz(1);buzz(2);fizz(3);buzz(4);sum(1, 2, 3, 4);", pullLog())

    return "OK"
}