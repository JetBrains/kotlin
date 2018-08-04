/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 10
 DESCRIPTION: 'When' with bound value and range expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with range expressions (IntRange) and 'else' branch (as expression).
fun case_1(value: IntRange?, value1: Int, value2: Int): String = when (value) {
    -100..-1000 -> ""
    -0..0 -> ""
    -100..9 -> ""
    10..100 -> ""
    101..value1 -> ""
    value1..value2 -> ""
    value2+1..102301 -> ""
    null -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with range expressions (IntRange) and without 'else' branch (as statement).
fun case_2(value: IntRange?, value1: Int, value2: Int): String {
    when (value) {
        -100..-1000 -> return ""
        -0..0 -> return ""
        -100..9 -> return ""
        10..100 -> return ""
        101..value1 -> return ""
        value1 + 1..value2 -> return ""
        null -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with range expressions (LongRange) and without 'else' branch (as statement).
fun case_3(value: LongRange?, value1: Long, value2: Long): String {
    when (value) {
        -100L..-1000L -> return ""
        -0L..0L -> return ""
        -100..9L -> return ""
        10L..100L -> return ""
        101L..value1 -> return ""
        value1..value2 -> return ""
        value2..102301239123L -> return ""
        null -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with range expressions (LongRange) and without 'else' branch (as statement).
fun case_3(value: CharRange?, value1: Char, value2: Char): String {
    when (value) {
        0.toChar()..10.toChar() -> return ""
        11.toChar()..100.toChar() -> return ""
        101.toChar()..value1 -> return ""
        value1 + 1..value2 -> return ""
        value2..255.toChar() -> return ""
        null -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with range expressions (Any) and without 'else' branch (as statement).
fun case_4(value: Any?, value1: Int, value2: Long): String {
    when (value) {
        -100..-1000 -> return ""
        -0..0 -> return ""
        -100..9 -> return ""
        10..100 -> return ""
        101..value1 -> return ""
        value1 + 1..value2 - 1 -> return ""
        value2..102301239123L -> return ""
        null -> return ""
    }

    return ""
}
