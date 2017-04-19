fun assertEquals(builder: StringBuilder, content: String) {
    if (builder.toString() != content) throw AssertionError("Test failed: " + builder.toString() + " != " + content)
}

fun assertTrue(condition: Boolean) {
    if (!condition) throw AssertionError("The condition is false")
}

// IndexOutOfBoundsException
fun assertException(body: () -> Unit) {
    try {
        body()
        throw AssertionError ("Test failed: no IndexOutOfBoundsException on wrong indices")
    } catch (e: IndexOutOfBoundsException) {}
}

fun String.toCharArray(): CharArray = CharArray(length) { i -> this[i] }

fun testInsertString(initial: String, index: Int, toInsert: String, expected: String) {
    assertEquals(StringBuilder(initial).insert(index, toInsert),                 expected)
    assertEquals(StringBuilder(initial).insert(index, toInsert.toCharArray()),   expected)
    assertEquals(StringBuilder(initial).insert(index, toInsert as CharSequence), expected)
}

fun testInsertStringException(initial: String, index: Int, toInsert: String) {
    assertException { StringBuilder(initial).insert(index, toInsert) }
    assertException { StringBuilder(initial).insert(index, toInsert.toCharArray()) }
    assertException { StringBuilder(initial).insert(index, toInsert as CharSequence) }
}

fun testInsertSingle(value: Byte) {
    assertEquals(StringBuilder("abcd").insert(0, value),  value.toString() + "abcd")
    assertEquals(StringBuilder("abcd").insert(4, value),  "abcd" + value.toString())
    assertEquals(StringBuilder("abcd").insert(2, value),  "ab" + value.toString() + "cd")
    assertEquals(StringBuilder("").insert(0, value),      value.toString())
}

fun testInsertSingle(value: Short) {
    assertEquals(StringBuilder("abcd").insert(0, value),  value.toString() + "abcd")
    assertEquals(StringBuilder("abcd").insert(4, value),  "abcd" + value.toString())
    assertEquals(StringBuilder("abcd").insert(2, value),  "ab" + value.toString() + "cd")
    assertEquals(StringBuilder("").insert(0, value),      value.toString())
}

fun testInsertSingle(value: Int) {
    assertEquals(StringBuilder("abcd").insert(0, value),  value.toString() + "abcd")
    assertEquals(StringBuilder("abcd").insert(4, value),  "abcd" + value.toString())
    assertEquals(StringBuilder("abcd").insert(2, value),  "ab" + value.toString() + "cd")
    assertEquals(StringBuilder("").insert(0, value),      value.toString())
}

fun testInsertSingle(value: Long) {
    assertEquals(StringBuilder("abcd").insert(0, value),  value.toString() + "abcd")
    assertEquals(StringBuilder("abcd").insert(4, value),  "abcd" + value.toString())
    assertEquals(StringBuilder("abcd").insert(2, value),  "ab" + value.toString() + "cd")
    assertEquals(StringBuilder("").insert(0, value),      value.toString())
}

fun testInsertSingle(value: Float) {
    assertEquals(StringBuilder("abcd").insert(0, value),  value.toString() + "abcd")
    assertEquals(StringBuilder("abcd").insert(4, value),  "abcd" + value.toString())
    assertEquals(StringBuilder("abcd").insert(2, value),  "ab" + value.toString() + "cd")
    assertEquals(StringBuilder("").insert(0, value),      value.toString())
}

fun testInsertSingle(value: Double) {
    assertEquals(StringBuilder("abcd").insert(0, value),  value.toString() + "abcd")
    assertEquals(StringBuilder("abcd").insert(4, value),  "abcd" + value.toString())
    assertEquals(StringBuilder("abcd").insert(2, value),  "ab" + value.toString() + "cd")
    assertEquals(StringBuilder("").insert(0, value),      value.toString())
}

fun testInsertSingle(value: Any?) {
    assertEquals(StringBuilder("abcd").insert(0, value),  value.toString() + "abcd")
    assertEquals(StringBuilder("abcd").insert(4, value),  "abcd" + value.toString())
    assertEquals(StringBuilder("abcd").insert(2, value),  "ab" + value.toString() + "cd")
    assertEquals(StringBuilder("").insert(0, value),      value.toString())
}

fun testInsertSingle(value: Char) {
    assertEquals(StringBuilder("abcd").insert(0, value),  value.toString() + "abcd")
    assertEquals(StringBuilder("abcd").insert(4, value),  "abcd" + value.toString())
    assertEquals(StringBuilder("abcd").insert(2, value),  "ab" + value.toString() + "cd")
    assertEquals(StringBuilder("").insert(0, value),      value.toString())
}

fun main(args: Array<String>) {
    // String/CharSequence/CharArray
    testInsertString("abcd", 0, "12", "12abcd")
    testInsertString("abcd", 4, "12", "abcd12")
    testInsertString("abcd", 2, "12", "ab12cd")
    testInsertString("", 0, "12", "12")
    testInsertStringException("a", -1, "1")
    testInsertStringException("a", 2, "1")

    // Subsequence of CharSequence
    // Insert in the beginning
    assertEquals(StringBuilder("abcd").insert(0, "1234", 0, 0), "abcd")     // 0 symbols
    assertEquals(StringBuilder("abcd").insert(0, "1234", 0, 1), "1abcd")    // 1 symbol
    assertEquals(StringBuilder("abcd").insert(0, "1234", 1, 3), "23abcd")   // 2 symbols

    // Insert in the end
    assertEquals(StringBuilder("abcd").insert(4, "1234", 0, 0), "abcd")
    assertEquals(StringBuilder("abcd").insert(4, "1234", 0, 1), "abcd1")
    assertEquals(StringBuilder("abcd").insert(4, "1234", 1, 3), "abcd23")

    // Insert in the middle
    assertEquals(StringBuilder("abcd").insert(2, "1234", 0, 0), "abcd")
    assertEquals(StringBuilder("abcd").insert(2, "1234", 0, 1), "ab1cd")
    assertEquals(StringBuilder("abcd").insert(2, "1234", 1, 3), "ab23cd")

    // Incorrect indices
    assertException { StringBuilder("a").insert(-1, "1", 0, 0) }
    assertException { StringBuilder("a").insert(2, "1", 0, 0) }
    assertException { StringBuilder("a").insert(1, "1", -1, 0) }
    assertException { StringBuilder("a").insert(1, "1", 0, 2) }
    assertException { StringBuilder("a").insert(1, "123", 2, 0) }


    // Other types
    testInsertSingle(true)
    testInsertSingle(42.toByte())
    testInsertSingle(42.toShort())
    testInsertSingle(42.toInt())
    testInsertSingle(42.toLong())
    testInsertSingle(42.2.toFloat())
    testInsertSingle(42.2.toDouble())
    testInsertSingle(object {
        override fun toString(): String {
            return "Object"
        }
    })
    testInsertSingle('a')

    println("OK")
}