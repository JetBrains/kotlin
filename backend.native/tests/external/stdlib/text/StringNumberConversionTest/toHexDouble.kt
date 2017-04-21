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

private fun doubleTotalOrderEquals(a: Any?, b: Any?) = a == b

fun box() {
    compareConversion(String::toDouble, String::toDoubleOrNull, ::doubleTotalOrderEquals) {
        assertProduces("0x77p1", (0x77 shl 1).toDouble())
        assertProduces("0x.77P8", 0x77.toDouble())

        // TODO: Java Double.valueOf specification requires mandatory binary exponent character (p) in the string parsed if the string is a hex one.
        // See: http://docs.oracle.com/javase/8/docs/api/java/lang/Double.html#valueOf-java.lang.String-
        // E.g.
        // "0x77p0".toDouble() // OK for both Kotlin/JVM and Kotlin/Native.
        // "0x77".toDouble()   // throws NumberFormatException in Kotlin/JVM and OK in Kotlin/Native.
        // Do we need to handle such case? Or it is OK to consume such strings?
        //assertFailsOrNull("0x77e1")
    }
}
