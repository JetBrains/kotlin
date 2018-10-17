/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, decimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 3
 DESCRIPTION: Integers with underscore symbols before the first digit (it's considered as identifiers).
 */

val value = _5678_90
val value = _2_3_4_5_6_7_8_9_
val value = _____________0000
val value = _______________________________________________________________________________________________________________________________________________________0
val value = ____________________________________________________
val value = _
val value = _0_
val value = _9_
