/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: constant-literals, integer-literals, binary-integer-literals
 * PARAGRAPH: 1
 * SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 * NUMBER: 1
 * DESCRIPTION: Binary integer literals with an underscore after the prefix.
 */

// TESTCASE NUMBER: 1
val value_1 = <!ILLEGAL_UNDERSCORE!>0b_1110100000<!>

// TESTCASE NUMBER: 2
val value_2 = <!ILLEGAL_UNDERSCORE!>0B_______11010000<!>

// TESTCASE NUMBER: 3
val value_3 = <!ILLEGAL_UNDERSCORE!>0B_1_1_0_1_0_0_0_0<!>

// TESTCASE NUMBER: 4
val value_4 = <!ILLEGAL_UNDERSCORE, INT_LITERAL_OUT_OF_RANGE!>0b_<!>
