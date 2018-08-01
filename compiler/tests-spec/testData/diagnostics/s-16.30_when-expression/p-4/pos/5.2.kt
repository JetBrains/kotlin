// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 5: Contains test condition: containment operator followed by an expression.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and containment operator on the classes with contains operator defined.
 */

class A {
    operator fun contains(a: Int): Boolean {
        return a > 30
    }

    operator fun contains(a: Long): Boolean {
        return a > 30L
    }

    operator fun contains(a: Char): Boolean {
        return a > 30.toChar()
    }

    fun getIntArray(value: Int): IntArray {
        return intArrayOf(1, 2, 3, value, 91923, 14, 123124)
    }

    fun getLongArray(value: Long): LongArray {
        return longArrayOf(1L, 2L, 3L, value, 9192323244L, 14L, 123124L)
    }

    fun getCharArray(value: Char): CharArray {
        return charArrayOf(1.toChar(), 2.toChar(), 3.toChar(), value, 91.toChar(), 14.toChar(), 123.toChar())
    }
}

// CASE DESCRIPTION: 'When' with contains operator on the classes with contains operator defined (IntRange).
fun case_1(value: Int, value1: List<IntArray>, value2: A, value3: IntRange): String {
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
fun case_2(value: Int, value1: List<IntArray>, value2: A): String = when (value) {
    in value1[0] -> ""
    in value1[10] -> ""
    in listOf(3, 5, 6, 7, 8) -> ""
    in value2 -> ""
    in value2.getIntArray(90000) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with one contains operator on the class with contains operator defined (IntRange).
fun case_3(value: Int, value1: A): String {
    when (value) {
        in value1.getIntArray(90000) -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with one contains operator on the class with contains operator defined (IntRange), and 'else' branch.
fun case_4(value: Int, value1: A): String = when (value) {
    in value1.getIntArray(90000) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with contains operator on the classes with contains operator defined (LongRange).
fun case_5(value: Long, value1: List<LongArray>, value2: A, value3: LongRange): String {
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
fun case_6(value: Long, value1: List<LongArray>, value2: A, value3: LongRange): String = when (value) {
    in value1[0] -> ""
    in value1[10] -> ""
    in listOf(3L, 5L, 6L, 7L, 8L) -> ""
    in value2 -> ""
    in value3 -> ""
    in value2.getLongArray(90000L) -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with contains operator on the classes with contains operator defined (IntRange).
fun case_7(value: Char, value1: List<CharArray>, value2: A, value3: CharRange): String {
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
fun case_8(value: Char, value1: List<CharArray>, value2: A, value3: CharRange): String = when (value) {
    in value1[0] -> ""
    in value1[10] -> ""
    in listOf(3.toChar(), 5.toChar(), 6.toChar(), 7.toChar(), 8.toChar()) -> ""
    in value2 -> ""
    in value3 -> ""
    in value2.getCharArray(90.toChar()) -> ""
    else -> ""
}