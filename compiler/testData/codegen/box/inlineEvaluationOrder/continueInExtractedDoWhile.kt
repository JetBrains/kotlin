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

private inline fun bar(predicate: (Char) -> Boolean): Int {
    var i = -1
    val str = "abc "
    do {
        i++
        if (i == 1) continue
        log(i.toString())
    } while (predicate(str[i]) && i < 3)
    return i
}

private fun test(c: Char): Int {
    return bar {
        log(it.toString())
        it != c
    }
}

fun box(): String {
    assertEquals(0, test('a'))
    assertEquals("0;a;", pullLog())

    assertEquals(1, test('b'))
    assertEquals("0;a;b;", pullLog())

    assertEquals(2, test('c'))
    assertEquals("0;a;b;2;c;", pullLog())

    assertEquals(3, test('*'))
    assertEquals("0;a;b;2;c;3; ;", pullLog())

    return "OK"
}