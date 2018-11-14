/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, binary-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] A sequence of binary digit symbols (0 or 1) prefixed by 0b or 0B is a binary integer literal.
 NUMBER: 1
 DESCRIPTION: Binary integer literals with the prefix only.
 */

val value_1 = <!INT_LITERAL_OUT_OF_RANGE!>0b<!>
val value_2 = <!INT_LITERAL_OUT_OF_RANGE!>0B<!>
