/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Various integer literals with not allowed long literal mark in lower case.
 */

// TESTCASE NUMBER: 1
val value_1 = 0<!WRONG_LONG_SUFFIX!>l<!>

// TESTCASE NUMBER: 2
val value_2 = 1000000000000000<!WRONG_LONG_SUFFIX!>l<!>

// TESTCASE NUMBER: 3
val value_3 = 0X0<!WRONG_LONG_SUFFIX!>l<!>

// TESTCASE NUMBER: 4
val value_4 = 0b101<!WRONG_LONG_SUFFIX!>l<!>
