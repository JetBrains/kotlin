
fun <T> assertEquals(a: T, b: T) { if (a != b) throw AssertionError("$a != $b") }

fun Double.Companion.MAX() = MAX_VALUE
fun Double.Companion.MIN() = MIN_VALUE

fun <T> test(o: T) { assertEquals(o === Double.Companion, true) }

fun box(): String {

    assertEquals(1.7976931348623157E308, Double.MAX_VALUE)

    assertEquals(Double.MIN_VALUE, Double.MIN())
    assertEquals(Double.MAX_VALUE, Double.Companion.MAX())

    test(Double)
    test(Double.Companion)

    return "OK"
}