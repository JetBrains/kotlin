// FIR_IDENTICAL
// DIAGNOSTICS: -UNREACHABLE_CODE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-253
 * MAIN LINK: statements, loop-statements, do-while-loop-statement -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: statements, loop-statements, do-while-loop-statement -> paragraph 3 -> sentence 1
 * statements, loop-statements, do-while-loop-statement -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: do-while-loop statement body contains zero statements
 */

// TESTCASE NUMBER: 1
fun case1() {
    do {
    } while (true)
}

// TESTCASE NUMBER: 2
fun case2() {
    do {
    } while (false)
}

// TESTCASE NUMBER: 3
fun case3() {
    do {
    } while (TODO())
}

