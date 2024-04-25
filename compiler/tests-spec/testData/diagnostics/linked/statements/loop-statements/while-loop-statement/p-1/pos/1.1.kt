// FIR_IDENTICAL
// DIAGNOSTICS: -UNREACHABLE_CODE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-253
 * MAIN LINK: statements, loop-statements, while-loop-statement -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: statements, loop-statements, while-loop-statement -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: while-loop statement body contains zero statements
 */

// TESTCASE NUMBER: 1
fun case1() {
    while (true) {
    }
}

// TESTCASE NUMBER: 2
fun case2() {
    while (false) {
    }
}

// TESTCASE NUMBER: 3
fun case3() {
    while (TODO()) {
    }
}

