/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 3
 * SENTENCE: [2] Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 * NUMBER: 2
 * DESCRIPTION: 'When' without bound value and only one 'else' branch.
 */

// TESTCASE NUMBER: 1
fun case_1() = when {
    else -> ""
}

// TESTCASE NUMBER: 2
fun case_2(): String {
    when {
        else -> return ""
    }
}