
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 3
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and enumeration of the containment operators.
 * HELPERS: typesProvider, classes
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: Int, value_3: Short): String {
    when (value_1) {
        in Long.MIN_VALUE..-100, in -99..0 -> return ""
        !in 100.toByte()..value_2, in value_2..value_3 -> return ""
    }

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int, value_2: List<IntArray>, value_3: Class) = when (value_1) {
    !in value_2[0], !in listOf(0, 1, 2, 3, 4), !in value_3.getIntArray() -> ""
    else -> ""
}