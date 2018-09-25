// IGNORE_BACKEND: JVM_IR

fun <T> assertEquals(a: T, b: T) { if (a != b) throw AssertionError("$a != $b") }

fun Int.Companion.MAX() = MAX_VALUE
fun Int.Companion.MIN() = MIN_VALUE

fun <T> test(o: T) { assertEquals(o === Int.Companion, true) }

fun box(): String {
    assertEquals(2147483647, Int.MAX_VALUE)

    assertEquals(Int.MIN_VALUE, Int.MIN())
    assertEquals(Int.MAX_VALUE, Int.Companion.MAX())

    test(Int)
    test(Int.Companion)

    return "OK"
}