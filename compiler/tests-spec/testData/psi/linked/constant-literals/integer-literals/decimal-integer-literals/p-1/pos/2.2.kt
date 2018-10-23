/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, decimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 2
 DESCRIPTION: Integers with underscore symbol in the last position.
 */

val value = 1_
val value = 1_00000000000000000_
val value = 1_____________
val value = 9____________0_
val value = 1_______________________________________________________________________________________________________________________________________________________
