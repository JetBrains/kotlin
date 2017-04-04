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
    compareConversion({it.toShort()}, {it.toShortOrNull()}) {
        assertProduces("+77", 77.toShort())
        assertProduces("32767", Short.MAX_VALUE)
        assertProduces("-32768", Short.MIN_VALUE)
        assertFailsOrNull("+32768")
        assertFailsOrNull("")
        assertFailsOrNull("   ")
    }

    compareConversionWithRadix(String::toShort, String::toShortOrNull) {
        assertProduces(16, "7FFF", 0x7FFF.toShort())
        assertProduces(16, "-8000", (-0x8000).toShort())
        assertFailsOrNull(5, "10000000")
        assertFailsOrNull(2, "")
        assertFailsOrNull(2, "   ")
    }
}
