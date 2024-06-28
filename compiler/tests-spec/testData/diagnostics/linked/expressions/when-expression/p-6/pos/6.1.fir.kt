// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 6
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and not allowed break and continue expression (without labels) in 'when condition'.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String {
    while (true) {
        when (value_1) {
            break -> return ""
        }
    }

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String {
    while (true) {
        when (value_1) {
            continue -> return ""
        }
    }

    return ""
}
