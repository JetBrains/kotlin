// IGNORE_BACKEND: JVM_IR

fun <T> assertEquals(a: T, b: T) { if (a != b) throw AssertionError("$a != $b") }

fun Short.Companion.MAX() = MAX_VALUE
fun Short.Companion.MIN() = MIN_VALUE

fun <T> test(o: T) { assertEquals(o === Short.Companion, true) }

fun box(): String {

    assertEquals(32767, Short.MAX_VALUE)

    assertEquals(Short.MIN_VALUE, Short.MIN())
    assertEquals(Short.MAX_VALUE, Short.Companion.MAX())

    test(Short)
    test(Short.Companion)

    return "OK"
}