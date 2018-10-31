/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 1
 * SENTENCE: [1] A real literal consists of the following parts: the whole-number part, the decimal point (ASCII period character .), the fraction part and the exponent.
 * NUMBER: 1
 * DESCRIPTION: Real literals separeted by comments.
 */

// TESTCASE NUMBER: 1
val value_1 = 0./**/<!ILLEGAL_SELECTOR!>99901<!>

// TESTCASE NUMBER: 2
val value_2 = 2./** some doc */<!ILLEGAL_SELECTOR!>1<!>

// TESTCASE NUMBER: 3
val value_3 = 9999./** some doc *//**/<!ILLEGAL_SELECTOR!>1<!>

// TESTCASE NUMBER: 4
val value_4 = 9999./** some /** some doc */ doc */<!ILLEGAL_SELECTOR!>1<!>

// TESTCASE NUMBER: 5
val value_5 = 9999./**/
<!ILLEGAL_SELECTOR!>1<!>

// TESTCASE NUMBER: 6
val value_6 = 1000000.//0
<!ILLEGAL_SELECTOR!>0<!>
