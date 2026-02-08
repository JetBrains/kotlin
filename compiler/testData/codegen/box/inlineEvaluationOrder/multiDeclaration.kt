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

// CHECK_NOT_CALLED: component2

class A(val x: Int, val y: Int)

operator fun A.component1(): Int = fizz(x)

inline operator fun A.component2(): Int = buzz(y)

fun box(): String {
    val (a, b) = A(1, 2)
    assertEquals(a, 1)
    assertEquals(b, 2)
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}