// WITH_STDLIB

// See KT-65230
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

fun toStringTemplateAny(x: Any) = " $x "
fun <T> toStringTemplateGeneric(x: T) = " $x "
inline fun <T> toStringTemplateInlineGeneric(x: T) = " $x "
inline fun <reified T> toStringTemplateInlineReifiedGeneric(x: T) = " $x "

class TestClass<T>(val x: T) {
    fun asString() = " $x "
    inline fun asStringInline() = " $x "
}

fun testByte() {
    val expectedMax = " 127 "
    val v1 = Byte.MAX_VALUE

    assertEquals(expectedMax, " $v1 ", "testByte - template v1")
    assertEquals(expectedMax, toStringTemplateAny(v1), "testByte - toStringTemplateAny(v1)")
    assertEquals(expectedMax, toStringTemplateGeneric(v1), "testByte - toStringTemplateGeneric(v1)")
    assertEquals(expectedMax, toStringTemplateInlineGeneric(v1), "testByte - toStringTemplateInlineGeneric(v1)")
    assertEquals(expectedMax, toStringTemplateInlineReifiedGeneric(v1), "testByte - toStringTemplateInlineReifiedGeneric(v1)")
    assertEquals(expectedMax, TestClass(v1).asString(), "testByte - TestClass(v1).asString()")
    assertEquals(expectedMax, TestClass(v1).asStringInline(), "testByte - TestClass(v1).asStringInline()")

    val expectedMin = " -128 "
    val v2 = Byte.MIN_VALUE

    assertEquals(expectedMin, " $v2 ", "testByte - template v2")
    assertEquals(expectedMin, toStringTemplateAny(v2), "testByte - toStringTemplateAny(v2)")
    assertEquals(expectedMin, toStringTemplateGeneric(v2), "testByte - toStringTemplateGeneric(v2)")
    assertEquals(expectedMin, toStringTemplateInlineGeneric(v2), "testByte - toStringTemplateInlineGeneric(v2)")
    assertEquals(expectedMin, toStringTemplateInlineReifiedGeneric(v2), "testByte - toStringTemplateInlineReifiedGeneric(v2)")
    assertEquals(expectedMin, TestClass(v2).asString(), "testByte - TestClass(v2).asString()")
    assertEquals(expectedMin, TestClass(v2).asStringInline(), "testByte - TestClass(v2).asStringInline()")
}

fun testInt() {
    val expectedMax = " 2147483647 "
    val v1 = Int.MAX_VALUE

    assertEquals(expectedMax, " $v1 ", "testInt - template v1")
    assertEquals(expectedMax, toStringTemplateAny(v1), "testInt - toStringTemplateAny(v1)")
    assertEquals(expectedMax, toStringTemplateGeneric(v1), "testInt - toStringTemplateGeneric(v1)")
    assertEquals(expectedMax, toStringTemplateInlineGeneric(v1), "testInt - toStringTemplateInlineGeneric(v1)")
    assertEquals(expectedMax, toStringTemplateInlineReifiedGeneric(v1), "testInt - toStringTemplateInlineReifiedGeneric(v1)")
    assertEquals(expectedMax, TestClass(v1).asString(), "testInt - TestClass(v1).asString()")
    assertEquals(expectedMax, TestClass(v1).asStringInline(), "testInt - TestClass(v1).asStringInline()")

    val expectedMin = " -2147483648 "
    val v2 = Int.MIN_VALUE

    assertEquals(expectedMin, " $v2 ", "testInt - template v2")
    assertEquals(expectedMin, toStringTemplateAny(v2), "testInt - toStringTemplateAny(v2)")
    assertEquals(expectedMin, toStringTemplateGeneric(v2), "testInt - toStringTemplateGeneric(v2)")
    assertEquals(expectedMin, toStringTemplateInlineGeneric(v2), "testInt - toStringTemplateInlineGeneric(v2)")
    assertEquals(expectedMin, toStringTemplateInlineReifiedGeneric(v2), "testInt - toStringTemplateInlineReifiedGeneric(v2)")
    assertEquals(expectedMin, TestClass(v2).asString(), "testInt - TestClass(v2).asString()")
    assertEquals(expectedMin, TestClass(v2).asStringInline(), "testInt - TestClass(v2).asStringInline()")
}

fun testLong() {
    val expectedMax = " 9223372036854775807 "
    val v1 = Long.MAX_VALUE

    assertEquals(expectedMax, " $v1 ", "testLong - template v1")
    assertEquals(expectedMax, toStringTemplateAny(v1), "testLong - toStringTemplateAny(v1)")
    assertEquals(expectedMax, toStringTemplateGeneric(v1), "testLong - toStringTemplateGeneric(v1)")
    assertEquals(expectedMax, toStringTemplateInlineGeneric(v1), "testLong - toStringTemplateInlineGeneric(v1)")
    assertEquals(expectedMax, toStringTemplateInlineReifiedGeneric(v1), "testLong - toStringTemplateInlineReifiedGeneric(v1)")
    assertEquals(expectedMax, TestClass(v1).asString(), "testLong - TestClass(v1).asString()")
    assertEquals(expectedMax, TestClass(v1).asStringInline(), "testLong - TestClass(v1).asStringInline()")

    val expectedMin = " -9223372036854775808 "
    val v2 = Long.MIN_VALUE

    assertEquals(expectedMin, " $v2 ", "testLong - template v2")
    assertEquals(expectedMin, toStringTemplateAny(v2), "testLong - toStringTemplateAny(v2)")
    assertEquals(expectedMin, toStringTemplateGeneric(v2), "testLong - toStringTemplateGeneric(v2)")
    assertEquals(expectedMin, toStringTemplateInlineGeneric(v2), "testLong - toStringTemplateInlineGeneric(v2)")
    assertEquals(expectedMin, toStringTemplateInlineReifiedGeneric(v2), "testLong - toStringTemplateInlineReifiedGeneric(v2)")
    assertEquals(expectedMin, TestClass(v2).asString(), "testLong - TestClass(v2).asString()")
    assertEquals(expectedMin, TestClass(v2).asStringInline(), "testLong - TestClass(v2).asStringInline()")
}

fun testULong() {
    val expectedMax = " 18446744073709551615 "
    val v = ULong.MAX_VALUE

    assertEquals(expectedMax, " $v ", "testULong - template")
    assertEquals(expectedMax, toStringTemplateAny(v), "testULong - toStringTemplateAny(v)")
    assertEquals(expectedMax, toStringTemplateGeneric(v), "testULong - toStringTemplateGeneric(v)")
    assertEquals(expectedMax, toStringTemplateInlineGeneric(v), "testULong - toStringTemplateInlineGeneric(v)")
    assertEquals(expectedMax, toStringTemplateInlineReifiedGeneric(v), "testULong - toStringTemplateInlineReifiedGeneric(v)")
    assertEquals(expectedMax, TestClass(v).asString(), "testULong - TestClass(v).asString()")
    assertEquals(expectedMax, TestClass(v).asStringInline(), "testULong - TestClass(v).asStringInline()")
}

fun testChar() {
    val expected = " c "
    val v = 'c'

    assertEquals(expected, " $v ", "testChar - template")
    assertEquals(expected, toStringTemplateAny(v), "testChar - toStringTemplateAny(v)")
    assertEquals(expected, toStringTemplateGeneric(v), "testChar - toStringTemplateGeneric(v)")
    assertEquals(expected, toStringTemplateInlineGeneric(v), "testChar - toStringTemplateInlineGeneric(v)")
    assertEquals(expected, toStringTemplateInlineReifiedGeneric(v), "testChar - toStringTemplateInlineReifiedGeneric(v)")
    assertEquals(expected, TestClass(v).asString(), "testChar - TestClass(v).asString()")
    assertEquals(expected, TestClass(v).asStringInline(), "testChar - TestClass(v).asStringInline()")
}

fun testArrayInt() {
    val expected = " -2147483648,0,2147483647 "
    val expectedShort = " [...] "
    val v = arrayOf(Int.MIN_VALUE, 0, Int.MAX_VALUE)

    assertEquals(expected, " $v ", "testArrayInt - template")
    assertEquals(expectedShort, toStringTemplateAny(v), "testArrayInt - toStringTemplateAny")
    assertEquals(expectedShort, toStringTemplateGeneric(v), "testArrayInt - toStringTemplateGeneric")
    assertEquals(expectedShort, toStringTemplateInlineGeneric(v), "testArrayInt - toStringTemplateInlineGeneric")
    assertEquals(expectedShort, toStringTemplateInlineReifiedGeneric(v), "testArrayInt - toStringTemplateInlineReifiedGeneric")
    assertEquals(expectedShort, TestClass(v).asString(), "testArrayInt - TestClass::asString")
    assertEquals(expectedShort, TestClass(v).asStringInline(), "testArrayInt - TestClass::asStringInline")
}

fun testArrayLong() {
    val expected = " -9223372036854775808,0,9223372036854775807 "
    val expectedShort = " [...] "
    val v = arrayOf(Long.MIN_VALUE, 0L, Long.MAX_VALUE)

    assertEquals(expected, " $v ", "testArrayLong - template")
    assertEquals(expectedShort, toStringTemplateAny(v), "testArrayLong - toStringTemplateAny")
    assertEquals(expectedShort, toStringTemplateGeneric(v), "testArrayLong - toStringTemplateGeneric")
    assertEquals(expectedShort, toStringTemplateInlineGeneric(v), "testArrayLong - toStringTemplateInlineGeneric")
    assertEquals(expectedShort, toStringTemplateInlineReifiedGeneric(v), "testArrayLong - toStringTemplateInlineReifiedGeneric")
    assertEquals(expectedShort, TestClass(v).asString(), "testArrayLong - TestClass::asString")
    assertEquals(expectedShort, TestClass(v).asStringInline(), "testArrayLong - TestClass::asStringInline")
}

class UserClass {
    override fun toString() = "Hello World!"

    @JsName("valueOf")
    fun valueOf() = "NOT OK!!!"
}

fun testUserClass() {
    val expected = " Hello World! "
    val v = UserClass()

    assertEquals(expected, " $v ", "testUserClass - template")
    assertEquals(expected, toStringTemplateAny(v), "testUserClass - toStringTemplateAny(v)")
    assertEquals(expected, toStringTemplateGeneric(v), "testUserClass - toStringTemplateGeneric(v)")
    assertEquals(expected, toStringTemplateInlineGeneric(v), "testUserClass - toStringTemplateInlineGeneric(v)")
    assertEquals(expected, toStringTemplateInlineReifiedGeneric(v), "testUserClass - toStringTemplateInlineReifiedGeneric(v)")
    assertEquals(expected, TestClass(v).asString(), "testUserClass - TestClass(v).asString()")
    assertEquals(expected, TestClass(v).asStringInline(), "testUserClass - TestClass(v).asStringInline()")
}

value class UserValueClass(val x: Int) {
    override fun toString() = "Hello World!"

    @JsName("valueOf")
    fun valueOf() = "NOT OK!!!"
}

fun testUserValueClass() {
    val expected = " Hello World! "
    val v = UserValueClass(1)

    assertEquals(expected, " $v ", "testUserValueClass - template")
    assertEquals(expected, toStringTemplateAny(v), "testUserValueClass - toStringTemplateAny(v)")
    assertEquals(expected, toStringTemplateGeneric(v), "testUserValueClass - toStringTemplateGeneric(v)")
    assertEquals(expected, toStringTemplateInlineGeneric(v), "testUserValueClass - toStringTemplateInlineGeneric(v)")
    assertEquals(expected, toStringTemplateInlineReifiedGeneric(v), "testUserValueClass - toStringTemplateInlineReifiedGeneric(v)")
    assertEquals(expected, TestClass(v).asString(), "testUserValueClass - TestClass(v).asString()")
    assertEquals(expected, TestClass(v).asStringInline(), "testUserValueClass - TestClass(v).asStringInline()")
}

fun box(): String {
    testByte()
    testInt()
    testLong()
    testULong()
    testChar()
    testArrayInt()
    testArrayLong()
    testUserClass()
    testUserValueClass()
    return "OK"
}
