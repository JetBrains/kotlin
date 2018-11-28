// !WITH_BASIC_TYPES
// !WITH_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 7
 * SENTENCE: [3] Contains test condition: containment operator followed by an expression.
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and enumeration of the containment operators.
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
fun case_2(value_1: Int, value_2: List<IntArray>, value_3: _Class) = when (value_1) {
    !in value_2[0], !in listOf(0, 1, 2, 3, 4), !in value_3.getIntArray(90) -> ""
    else -> ""
}