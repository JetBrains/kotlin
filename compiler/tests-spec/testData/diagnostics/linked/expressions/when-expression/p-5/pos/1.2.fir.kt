// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, when-expression -> paragraph 5 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and allowed break and continue expression (without labels) in the control structure body.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): Int {
    while (true) {
        when (value_1) {
            1 -> return 1
            2 -> break
        }
    }

    return 0
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): Int {
    while (true) {
        when (value_1) {
            1 -> continue
            2 -> return 1
        }
    }

    return 0
}
