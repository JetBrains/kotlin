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

private fun doubleTotalOrderEquals(a: Double?, b: Double?): Boolean {
    if (a != null && b != null && a.isNaN() && b.isNaN()) {
        return true
    }
    return a == b
}

fun box() {
    compareConversion(String::toDouble, String::toDoubleOrNull, ::doubleTotalOrderEquals) {
        assertProduces("-77", -77.0)
        assertProduces("77.", 77.0)
        assertProduces("77.0", 77.0)
        assertProduces("-1.77", -1.77)
        assertProduces("+.77", 0.77)
        assertProduces("\t-77 \n", -77.0)
        assertProduces("7.7e1", 77.0)
        assertProduces("+770e-1", 77.0)

        assertProduces("-NaN", -Double.NaN)
        assertProduces("+Infinity", Double.POSITIVE_INFINITY)

        assertFailsOrNull("7..7")
        assertFailsOrNull("007 not a number")
        assertFailsOrNull("")
        assertFailsOrNull("   ")
    }
}
