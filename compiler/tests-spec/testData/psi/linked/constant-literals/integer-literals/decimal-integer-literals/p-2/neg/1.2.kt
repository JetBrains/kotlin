/*
 KOTLIN PSI SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, decimal-integer-literals
 PARAGRAPH: 2
 SENTENCE: [1] Even more so, any decimal literal starting with digit 0 and containing more than 1 digit is not a valid decimal literal.
 NUMBER: 2
 DESCRIPTION: Integer literals with 0 in the first position and it contains more than 1 digit separated by underscore.
 */

val value = 0_0
val value = 000_000
val value = 0_10000
val value = 0_1000100
val value = 0_99999999
