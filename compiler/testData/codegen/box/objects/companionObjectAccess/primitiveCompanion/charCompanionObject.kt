// IGNORE_BACKEND: JVM_IR

fun <T> assertEquals(a: T, b: T) { if (a != b) throw AssertionError("$a != $b") }

fun Char.Companion.MAX() = MAX_SURROGATE
fun Char.Companion.MIN() = MIN_SURROGATE

fun <T> test(o: T) { assertEquals(o === Char.Companion, true) }

fun box(): String {

    assertEquals('\uDFFF', Char.MAX_SURROGATE)

    assertEquals(Char.MIN_SURROGATE, Char.MIN())
    assertEquals(Char.MAX_SURROGATE, Char.Companion.MAX())

    test(Char)
    test(Char.Companion)

    return "OK"
}