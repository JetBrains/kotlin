// !WITH_CLASSES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 5: Contains test condition: containment operator followed by an expression.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and containment operator on the classes with contains operator defined.
 */

// CASE DESCRIPTION: 'When' with contains operator on the classes with contains operator defined (IntRange).
fun case_1(value: Int, value1: List<IntArray>, value2: _Class, value3: IntRange): String {
    when (value) {
        in value1[0] -> return ""
        in value1[10] -> return ""
        in listOf(3, 5, 6, 7, 8) -> return ""
        in value2 -> return ""
        in value3 -> return ""
        in value2.getIntArray(90000) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with contains operator on the classes with contains operator defined (IntRange), and 'else' branch.
fun case_2(value: Int, value1: List<IntArray>, value2: _Class): String = when (value) {
    in value1[0] -> ""
    in value1[10] -> ""
    in listOf(3, 5, 6, 7, 8) -> ""
    in value2 -> ""
    in value2.getIntArray(90000) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with one contains operator on the class with contains operator defined (IntRange).
fun case_3(value: Int, value1: _Class): String {
    when (value) {
        in value1.getIntArray(90000) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with one contains operator on the class with contains operator defined (IntRange), and 'else' branch.
fun case_4(value: Int, value1: _Class): String = when (value) {
    in value1.getIntArray(90000) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with contains operator on the classes with contains operator defined (LongRange).
fun case_5(value: Long, value1: List<LongArray>, value2: _Class, value3: LongRange): String {
    when (value) {
        in value1[0] -> return ""
        in value1[10] -> return ""
        in listOf(3L, 5L, 6L, 7L, 8L) -> return ""
        in value2 -> return ""
        in value3 -> return ""
        in value2.getLongArray(90000L) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with contains operator on the classes with contains operator defined (LongRange), and 'else' branch.
fun case_6(value: Long, value1: List<LongArray>, value2: _Class, value3: LongRange): String = when (value) {
    in value1[0] -> ""
    in value1[10] -> ""
    in listOf(3L, 5L, 6L, 7L, 8L) -> ""
    in value2 -> ""
    in value3 -> ""
    in value2.getLongArray(90000L) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with contains operator on the classes with contains operator defined (IntRange).
fun case_7(value: Char, value1: List<CharArray>, value2: _Class, value3: CharRange): String {
    when (value) {
        in value1[0] -> return ""
        in value1[10] -> return ""
        in listOf(3.toChar(), 5.toChar(), 6.toChar(), 7.toChar(), 8.toChar()) -> return ""
        in value2 -> return ""
        in value3 -> return ""
        in value2.getCharArray(90.toChar()) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with contains operator on the classes with contains operator defined (IntRange), and 'else' branch.
fun case_8(value: Char, value1: List<CharArray>, value2: _Class, value3: CharRange): String = when (value) {
    in value1[0] -> ""
    in value1[10] -> ""
    in listOf(3.toChar(), 5.toChar(), 6.toChar(), 7.toChar(), 8.toChar()) -> ""
    in value2 -> ""
    in value3 -> ""
    in value2.getCharArray(90.toChar()) -> ""
    else -> ""
}