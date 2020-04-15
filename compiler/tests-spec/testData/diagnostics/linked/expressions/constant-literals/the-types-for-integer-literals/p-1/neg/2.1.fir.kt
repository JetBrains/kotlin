// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, constant-literals, the-types-for-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Various integer literals with not allowed long literal mark in lower case (type checking).
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case_1() {
    0l checkType { check<Long>() }
    10000000000000l checkType { check<Long>() }
    0X000Af10cDl checkType { check<Long>() }
    0x0_0l checkType { check<Long>() }
    0b100_000_111_111l checkType { check<Long>() }
    0b0l checkType { check<Long>() }
}
