/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Various integer literals with a not allowed underscore before the long literal mark.
 */

// TESTCASE NUMBER: 1
val value_1 = <!ILLEGAL_UNDERSCORE!>0b0_<!WRONG_LONG_SUFFIX!>l<!><!>

// TESTCASE NUMBER: 2
val value_2 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0B12_L<!>

// TESTCASE NUMBER: 3
val value_3 = <!ILLEGAL_UNDERSCORE!>0X234_<!WRONG_LONG_SUFFIX!>l<!><!>

// TESTCASE NUMBER: 4
val value_4 = <!ILLEGAL_UNDERSCORE!>0x3567_L<!>
