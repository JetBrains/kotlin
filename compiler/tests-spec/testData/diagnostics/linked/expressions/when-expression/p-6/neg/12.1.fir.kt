// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-201
 * PLACE: expressions, when-expression -> paragraph 6 -> sentence 12
 * NUMBER: 1
 * DESCRIPTION: 'When' without bound value and with 'else' branch not in the last position.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String = when {
    else -> ""
    value_1 == 1 -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String = when {
    value_1 == 1 -> ""
    else -> ""
    value_1 == 2 -> ""
}

// TESTCASE NUMBER: 3
fun case_3(): String {
    when {
        else -> return ""
        else -> return ""
    }

    return ""
}
