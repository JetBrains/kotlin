import kotlin.test.*


private fun <T : Any> compareConversion(convertOrFail: (String) -> T,
                                        convertOrNull: (String) -> T?,
                                        equality: (T, T?) -> Boolean = { a, b -> a == b },
                                        assertions: ConversionContext<T>.() -> Unit) {
    ConversionContext(convertOrFail, convertOrNull, equality).assertions()
}


private fun <T : Any> compareConversionWithRadix(convertOrFail: String.(Int) -> T,
                                                 convertOrNull: String.(Int) -> T?,
                                                 assertions: ConversionWithRadixContext<T>.() -> Unit) {
    ConversionWithRadixContext(convertOrFail, convertOrNull).assertions()
}


private class ConversionContext<T: Any>(val convertOrFail: (String) -> T,
                                        val convertOrNull: (String) -> T?,
                                        val equality: (T, T?) -> Boolean) {

    private fun assertEquals(expected: T, actual: T?, input: String, operation: String) {
        assertTrue(equality(expected, actual), "Expected $operation('$input') to produce $expected but was $actual")
    }

    fun assertProduces(input: String, output: T) {
        assertEquals(output, convertOrFail(input), input, "convertOrFail")
        assertEquals(output, convertOrNull(input), input, "convertOrNull")
    }

    fun assertFailsOrNull(input: String) {
        assertFailsWith<NumberFormatException>("Expected to fail on input \"$input\"") { convertOrFail(input) }
        assertNull(convertOrNull(input), message = "On input \"$input\"")
    }
}

private class ConversionWithRadixContext<T: Any>(val convertOrFail: (String, Int) -> T,
                                                 val convertOrNull: (String, Int) -> T?) {
    fun assertProduces(radix: Int, input: String, output: T) {
        assertEquals(output, convertOrFail(input, radix))
        assertEquals(output, convertOrNull(input, radix))
    }

    fun assertFailsOrNull(radix: Int, input: String) {
        assertFailsWith<NumberFormatException>("Expected to fail on input \"$input\" with radix $radix",
                                               { convertOrFail(input, radix) })

        assertNull(convertOrNull(input, radix), message = "On input \"$input\" with radix $radix")
    }
}

fun box() {
    compareConversion({it.toInt()}, {it.toIntOrNull()}) {
        assertProduces("77", 77)
        assertProduces("+2147483647", Int.MAX_VALUE)
        assertProduces("-2147483648", Int.MIN_VALUE)

        assertFailsOrNull("2147483648")
        assertFailsOrNull("-2147483649")
        assertFailsOrNull("239239kotlin")
        assertFailsOrNull("")
        assertFailsOrNull("   ")
    }

    compareConversionWithRadix(String::toInt, String::toIntOrNull) {
        assertProduces(10, "0", 0)
        assertProduces(10, "473", 473)
        assertProduces(10, "+42", 42)
        assertProduces(10, "-0", 0)
        assertProduces(10, "2147483647", 2147483647)
        assertProduces(10, "-2147483648", -2147483648)

        assertProduces(16, "-FF", -255)
        assertProduces(16, "-ff", -255)
        assertProduces(2, "1100110", 102)
        assertProduces(27, "Kona", 411787)

        assertFailsOrNull(10, "2147483648")
        assertFailsOrNull(8, "99")
        assertFailsOrNull(10, "Kona")
        assertFailsOrNull(16, "")
        assertFailsOrNull(16, "  ")
    }

    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { "1".toInt(radix = 1) }
    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { "37".toIntOrNull(radix = 37) }
}
