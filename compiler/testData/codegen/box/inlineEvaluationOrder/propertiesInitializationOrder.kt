package foo
import kotlin.test.*

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

class A(val x: Int = fizz(1) + 1) {
    val y = buzz(x) + 1
    val z: Int

    init {
        z = fizz(x) + buzz(y)
    }
}

fun box(): String {
    val a = A()
    assertEquals(2, a.x)
    assertEquals(3, a.y)
    assertEquals(5, a.z)
    assertEquals("fizz(1);buzz(2);fizz(2);buzz(3);", pullLog())

    return "OK"
}