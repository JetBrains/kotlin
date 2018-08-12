// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_BASIC_TYPES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 1
 DESCRIPTION: 'When' with different variants of the arithmetic expressions (additive expression and multiplicative expression) in the control structure body.
 */

// CASE DESCRIPTION: 'When' with control structure body as arithmetic expression with Short.
fun case_1(value: Int, value1: Short, value2: _BasicTypesProvider) {
    val value3: Short = 32767
    val value4: Short = -32768

    when {
        value == 1 -> 900.toShort()
        value == 2 -> value2.getShort(value1.toInt()) - 9234.toShort()
        value == 3 -> 9234.toShort() * 0.toShort()
        value == 4 -> -6.toShort() / getShort(-9000)
        value == 5 -> 6.toShort() % 112.toShort()
        value == 5 -> -9313.toShort() % 10.toShort()
        value == 6 -> 6.toShort() - value4
        value == 7 -> 50.toShort() + value1 * -90.toShort() / value3 % 112.toShort() - value1
        value == 8 -> {
            value1 * -112.toShort() / 9234.toShort() - -1.toShort() + value2.getShort(111) / 0.toShort() % -99.toShort() % 9234.toShort() + value4
        }
    }
}

// CASE DESCRIPTION: 'When' with control structure body as arithmetic expression with Int.
fun case_2(value: Int, value1: Int, value2: _BasicTypesProvider) {
    val value3 = 912
    val value4 = 124901924904

    when {
        value == 1 -> 2
        value == 2 -> 2 + 2
        value == 3 -> 2 * -2
        value == 4 -> getInt(-9) / 3
        value == 5 -> -8 % 3
        value == 6 -> -4 - 2
        value == 7 -> value2.getInt(111) + 2 * getInt(2) / -2 % 2 - 2
        value == 8 -> 32 shl 2
        value == 9 -> value1 shr -value3
        value == 10 -> -64 ushr value3
        value == 11 -> value3 and 4
        value == 12 -> 16 or -5
        value == 13 -> value1 xor 55
        value == 14 -> -55.inv()
        value == 15 -> value1 * -value3
        value == 16 -> {
            value1 * 2 / 10 - 5 + value2.getInt(-500) / 2 % -4 % value4
        }
    }
}

// CASE DESCRIPTION: 'When' with control structure body as arithmetic expression with Long.
fun case_3(value: Int, value1: Long, value2: _BasicTypesProvider) {
    val value3 = 9L
    val value4 = 124909249042341234L

    when {
        value == 1 -> 2L
        value == 2 -> 1249011249042341234L + 412L
        value == 3 -> -2L * getLong(1000)
        value == 4 -> 3241019249042341234L / -2L
        value == 5 -> 324901924942341234L % -2L
        value == 6 -> 4L - value2.getLong(0)
        value == 7 -> 2L + -9L * -10000000000000L / 2L % value2.getLong(-99999999) - 2L
        value == 8 -> value1 * value3
        value == 9 -> {
            value1 * 2L / -10L - 92490149042341234L + 1324019249042341234L / 2L % 234124312423452L % value4
        }
    }
}

// CASE DESCRIPTION: 'When' with control structure body as arithmetic expression with Float.
fun case_4(value: Int, value1: Float, value2: _BasicTypesProvider) {
    val value3 = 912.2134F
    val value4 = -124901924904.991242f

    when {
        value == 1 -> 2.1F
        value == 2 -> 2.1f + value2.getFloat(-1)
        value == 3 -> getFloat(-10) * 2f
        value == 4 -> 8.5f / -2.3f
        value == 5 -> value2.getFloat(1111111) % 2f
        value == 6 -> 4.0F - 2.1f
        value == 7 -> 2f + .9f * -.0000000001F / 2F % 2.91f - -2.09F
        value == 8 -> value1 * -value3
        value == 9 -> {
            value1 * 2F / -10.12414141f - .13104141040f + 0.5F / -2F % 4.0f % value4
        }
    }
}

// CASE DESCRIPTION: 'When' with control structure body as arithmetic expression with Double.
fun case_5(value: Int, value1: Double, value2: _BasicTypesProvider) {
    val value3 = 912.2134
    val value4 = -124901924904.99124212

    when {
        value == 1 -> 2.1
        value == 2 -> 2.1 + 2.5
        value == 3 -> getDouble(-20) * 2.0
        value == 4 -> -8.5 / 2.3
        value == 5 -> 8.0 % value2.getDouble(100000000)
        value == 6 -> 4.0 - -2.1
        value == 7 -> 2.4 + -.9 * -.0000000001 / value2.getDouble(-10) % 2.91 - 2.09
        value == 8 -> -value1 * value3
        value == 9 -> {
            value1 * -2.0 / 10.12414141 - .13104141040 + 0.5 / 2.0 % 4.0 % value4
        }
    }
}

// CASE DESCRIPTION: 'When' with control structure body as arithmetic expression with Byte.
fun case_6(value: Int, value1: Byte, value2: _BasicTypesProvider) {
    val value3: Byte = -11
    val value4: Byte = 3
    val value5: Byte = 127
    val value6: Byte = 5
    val value7: Byte = -128

    when {
        value == 1 -> value1
        value == 2 -> -11.toByte() - value3
        value == 3 -> 90.toByte() * -value5
        value == 4 -> 2.toByte() / 100.toByte()
        value == 5 -> value4 % value5
        value == 6 -> 0.toByte() - 0.toByte()
        value == 7 -> value3 + -128.toByte() * 127.toByte() / getByte(-9999) % value5 - value2.getByte(9999999)
        value == 8 -> {
            value2.getByte(-100) * value5 / -10.toByte() - value6 + value4 / 9.toByte() % value5 % value1 + value7
        }
    }
}

// CASE DESCRIPTION: 'When' with control structure body as arithmetic expression (minus and plus of Integer) with Char.
fun case_7(value: Int, value1: Char, value2: _BasicTypesProvider) {
    val value3: Char = 11.toChar()

    when {
        value == 1 -> value1
        value == 2 -> 11.toChar() - 10
        value == 3 -> 0.toChar() + 92
        value == 3 -> value3 + 11
        value == 3 -> getChar(9) - 11
        value == 4 -> {
            value2.getChar(99) - 11 + 90
        }
    }
}