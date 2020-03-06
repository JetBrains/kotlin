
fun <T> assertEquals(a: T, b: T) { if (a != b) throw AssertionError("$a != $b") }

fun Float.Companion.MAX() = POSITIVE_INFINITY
fun Float.Companion.MIN() = NEGATIVE_INFINITY

fun <T> test(o: T) { assertEquals(o === Float.Companion, true) }

fun box(): String {

    assertEquals(1.0f / 0.0f, Float.POSITIVE_INFINITY)

    assertEquals(Float.NEGATIVE_INFINITY, Float.MIN())
    assertEquals(Float.POSITIVE_INFINITY, Float.Companion.MAX())

    test(Float)
    test(Float.Companion)

    return "OK"
}