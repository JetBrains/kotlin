/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Hexadecimal integer literals with an underscore after the prefix.
 */

// TESTCASE NUMBER: 1
val value_1 = 0x_1234567890

// TESTCASE NUMBER: 2
val value_2 = 0X_______23456789

// TESTCASE NUMBER: 3
val value_3 = 0X_2_3_4_5_6_7_8_9

// TESTCASE NUMBER: 4
val value_4 = <!ILLEGAL_CONST_EXPRESSION!>0x_<!>
