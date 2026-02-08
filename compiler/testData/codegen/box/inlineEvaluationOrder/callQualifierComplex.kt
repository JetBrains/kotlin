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

// CHECK_NOT_CALLED: multiplyFunInline

fun multiplyFun(): (Int, Int)->Int {
    log("multiplyFun()")
    return { x, y -> x * y }
}

inline
fun multiplyFunInline(): (Int, Int)->Int {
    log("multiplyFunInline()")
    return { x, y -> x * y }
}

fun box(): String {
    assertEquals(6, arrayOf(multiplyFun(), multiplyFunInline())[0](fizz(2), fizz(3)))
    assertEquals("multiplyFun();multiplyFunInline();fizz(2);fizz(3);", pullLog())

    return "OK"
}