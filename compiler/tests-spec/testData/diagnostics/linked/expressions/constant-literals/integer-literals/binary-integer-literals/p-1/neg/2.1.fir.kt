/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Binary integer literals with an underscore after the prefix.
 */

// TESTCASE NUMBER: 1
val value_1 = 0b_1110100000

// TESTCASE NUMBER: 2
val value_2 = 0B_______11010000

// TESTCASE NUMBER: 3
val value_3 = 0B_1_1_0_1_0_0_0_0

// TESTCASE NUMBER: 4
val value_4 = <!ILLEGAL_CONST_EXPRESSION!>0b_<!>
