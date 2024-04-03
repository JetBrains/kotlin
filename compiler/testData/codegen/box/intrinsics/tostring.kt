// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun assertEquals(expected: String, actual: String, message: String) {
    if (expected != actual) {
        throw AssertionError("${message}: expected $expected, actual $actual")
    }
}

fun <T : Any> T.myToString(): String = toString()

fun box(): String {
    assertEquals(239.toByte().toString(), (239.toByte() as Byte?).toString(), "byte failed")
    assertEquals(239.toShort().toString(), (239.toShort() as Short?).toString(), "short failed")
    assertEquals(239.toInt().toString(), (239.toInt() as Int?).toString(), "int failed")
    assertEquals(239.toFloat().toString(),  (239.toFloat() as Float?).toString(), "float failed")
    assertEquals(239.toLong().toString(), (239.toLong() as Long?).toString(), "long failed")
    assertEquals(239.toDouble().toString(), (239.toDouble() as Double?).toString(), "double failed")
    assertEquals(true.toString(), (true as Boolean?).toString(), "boolean failed")
    assertEquals('a'.toChar().toString(), ('a'.toChar() as Char?).toString(),  "char failed")

    assertEquals("${239.toByte()}", (239.toByte() as Byte?).toString(), "byte template failed")
    assertEquals("${239.toShort()}", (239.toShort() as Short?).toString(), "short template failed")
    assertEquals("${239.toInt()}", (239.toInt() as Int?).toString(), "int template failed")
    assertEquals("${239.toFloat()}", (239.toFloat() as Float?).toString(), "float template failed")
    assertEquals("${239.toLong()}", (239.toLong() as Long?).toString(), "long template failed")
    assertEquals("${239.toDouble()}", (239.toDouble() as Double?).toString(), "double template failed")
    assertEquals("${true}", (true as Boolean?).toString(), "boolean template failed")
    assertEquals("${'a'.toChar()}", ('a'.toChar() as Char?).toString(), "char template failed")

    for(b in 0..255) {
        assertEquals("${b.toByte()}", (b.toByte() as Byte?).toString(), "byte conversion failed")
    }

    assertEquals(23.21.myToString(), 23.21.toString(), "23.21 failed")

    assertEquals(Double.MIN_VALUE.myToString(), Double.MIN_VALUE.toString(), "Double.MIN_VALUE failed")
    assertEquals(Double.MAX_VALUE.myToString(), Double.MAX_VALUE.toString(), "Double.MAX_VALUE failed")
    assertEquals(Double.NEGATIVE_INFINITY.myToString(), Double.NEGATIVE_INFINITY.toString(), "Double.NEGATIVE_INFINITY failed")
    assertEquals(Double.POSITIVE_INFINITY.myToString(), Double.POSITIVE_INFINITY.toString(), "Double.POSITIVE_INFINITY failed")

    assertEquals(Float.MIN_VALUE.myToString(), Float.MIN_VALUE.toString(), "Float.MIN_VALUE failed")
    assertEquals(Float.MAX_VALUE.myToString(), Float.MAX_VALUE.toString(), "Float.MAX_VALUE failed")

    return "OK"
}
