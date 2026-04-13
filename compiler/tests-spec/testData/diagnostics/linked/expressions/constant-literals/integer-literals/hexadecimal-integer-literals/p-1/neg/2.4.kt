/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 4
 * DESCRIPTION: Hexadecimal integer literals with an underscore in the last position.
 */

// TESTCASE NUMBER: 1
val value_1 = <!ILLEGAL_UNDERSCORE!>0x3_4_5_6_7_8_____<!>

// TESTCASE NUMBER: 2
val value_2 = <!ILLEGAL_UNDERSCORE!>0X4_______5_______6_______7_<!>

// TESTCASE NUMBER: 3
val value_3 = <!ILLEGAL_UNDERSCORE!>0X000000000_<!>

// TESTCASE NUMBER: 5
val value_5 = <!ILLEGAL_UNDERSCORE!>0x_<!>

// TESTCASE NUMBER: 6
val value_6 = <!ILLEGAL_UNDERSCORE!>0X______________<!>

// TESTCASE NUMBER: 7
val value_7 = <!ILLEGAL_UNDERSCORE!>0X0_<!>

// TESTCASE NUMBER: 8
val value_8 = <!ILLEGAL_UNDERSCORE!>0X10_<!>
