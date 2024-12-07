/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, the-types-for-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Binary and hexadecimal integer literals with a long literal mark only.
 */

// TESTCASE NUMBER: 1
val value_1 = <!INT_LITERAL_OUT_OF_RANGE!>0bl<!>

// TESTCASE NUMBER: 2
val value_2 = <!INT_LITERAL_OUT_OF_RANGE!>0BL<!>

// TESTCASE NUMBER: 3
val value_3 = <!INT_LITERAL_OUT_OF_RANGE!>0Xl<!>

// TESTCASE NUMBER: 4
val value_4 = <!INT_LITERAL_OUT_OF_RANGE!>0xL<!>

// TESTCASE NUMBER: 5
val value_5 = <!ILLEGAL_UNDERSCORE!>0b_l<!>

// TESTCASE NUMBER: 6
val value_6 = <!ILLEGAL_UNDERSCORE!>0B_L<!>

// TESTCASE NUMBER: 7
val value_7 = <!ILLEGAL_UNDERSCORE!>0X____l<!>

// TESTCASE NUMBER: 8
val value_8 = <!ILLEGAL_UNDERSCORE!>0x_L<!>
