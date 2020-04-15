/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, constant-literals, the-types-for-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Various integer literals with a not allowed underscore before the long literal mark.
 */

// TESTCASE NUMBER: 1
val value_1 = 0b0_l

// TESTCASE NUMBER: 2
val value_2 = <!ILLEGAL_CONST_EXPRESSION!>0B12_L<!>

// TESTCASE NUMBER: 3
val value_3 = 0X234_l

// TESTCASE NUMBER: 4
val value_4 = 0x3567_L
