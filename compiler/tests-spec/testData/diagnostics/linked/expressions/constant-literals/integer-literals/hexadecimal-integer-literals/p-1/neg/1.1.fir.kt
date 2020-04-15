/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Hexadecimal integer literals with the prefix only.
 */

// TESTCASE NUMBER: 1
val value_1 = <!ILLEGAL_CONST_EXPRESSION!>0x<!>

// TESTCASE NUMBER: 2
val value_2 = <!ILLEGAL_CONST_EXPRESSION!>0X<!>
