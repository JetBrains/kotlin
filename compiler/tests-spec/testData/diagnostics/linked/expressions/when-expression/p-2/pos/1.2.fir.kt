// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, when-expression -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Allowed break and continue in the control structure body of when.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String {
    while (true) {
        when {
            value_1 == 1 -> break
        }
    }

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String {
    while (true) {
        when {
            value_1 == 1 -> continue
        }
    }

    return ""
}
