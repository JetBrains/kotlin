// IGNORE_BACKEND: JVM_IR

fun <T> assertEquals(a: T, b: T) { if (a != b) throw AssertionError("$a != $b") }

fun Long.Companion.MAX() = MAX_VALUE
fun Long.Companion.MIN() = MIN_VALUE

fun <T> test(o: T) { assertEquals(o === Long.Companion, true) }

fun box(): String {

    assertEquals(9223372036854775807L, Long.MAX_VALUE)

    assertEquals(Long.MIN_VALUE, Long.MIN())
    assertEquals(Long.MAX_VALUE, Long.Companion.MAX())

    test(Long)
    test(Long.Companion)

    return "OK"
}