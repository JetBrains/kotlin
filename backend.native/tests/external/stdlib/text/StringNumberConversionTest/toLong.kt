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
    compareConversion({it.toLong()}, {it.toLongOrNull()}) {
        assertProduces("77", 77.toLong())
        assertProduces("+9223372036854775807", Long.MAX_VALUE)
        assertProduces("-9223372036854775808", Long.MIN_VALUE)

        assertFailsOrNull("9223372036854775808")
        assertFailsOrNull("-9223372036854775809")
        assertFailsOrNull("922337 75809")
        assertFailsOrNull("92233,75809")
        assertFailsOrNull("92233`75809")
        assertFailsOrNull("-922337KOTLIN775809")
        assertFailsOrNull("")
        assertFailsOrNull("  ")
    }

    compareConversionWithRadix(String::toLong, String::toLongOrNull) {
        assertProduces(10, "0", 0L)
        assertProduces(10, "473", 473L)
        assertProduces(10, "+42", 42L)
        assertProduces(10, "-0", 0L)

        assertProduces(16, "7F11223344556677", 0x7F11223344556677)
        assertProduces(16, "+7faabbccddeeff00", 0x7faabbccddeeff00)
        assertProduces(16, "-8000000000000000", Long.MIN_VALUE)
        assertProduces(2, "1100110", 102L)
        assertProduces(36, "Hazelnut", 1356099454469L)

        assertFailsOrNull(8, "99")
        assertFailsOrNull(10, "Hazelnut")
        assertFailsOrNull(4, "")
        assertFailsOrNull(4, "  ")
    }

    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { "37".toLong(radix = 37) }
    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { "1".toLongOrNull(radix = 1) }
}
