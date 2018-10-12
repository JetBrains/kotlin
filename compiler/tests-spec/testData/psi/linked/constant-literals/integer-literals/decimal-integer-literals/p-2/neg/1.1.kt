/*
 KOTLIN PSI SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, decimal-integer-literals
 PARAGRAPH: 2
 SENTENCE: [1] Even more so, any decimal literal starting with digit 0 and containing more than 1 digit is not a valid decimal literal.
 NUMBER: 1
 DESCRIPTION: Integer literals with 0 in the first position and it contains more than 1 digit.
 */

val value = 00
val value = 000000
val value = 010000
val value = 01000100
val value = 099999999
