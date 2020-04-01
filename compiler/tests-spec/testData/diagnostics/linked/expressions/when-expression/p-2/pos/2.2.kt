// FIR_IDENTICAL
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, when-expression -> paragraph 2 -> sentence 2
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