// IGNORE_BACKEND: JVM_IR

fun <T> assertEquals(a: T, b: T) { if (a != b) throw AssertionError("$a != $b") }

fun Byte.Companion.MAX() = MAX_VALUE
fun Byte.Companion.MIN() = MIN_VALUE

fun <T> test(o: T) { assertEquals(o === Byte.Companion, true) }

fun box(): String {

    assertEquals(127, Byte.MAX_VALUE)

    assertEquals(Byte.MIN_VALUE, Byte.MIN())
    assertEquals(Byte.MAX_VALUE, Byte.Companion.MAX())

    test(Byte)
    test(Byte.Companion)

    return "OK"
}