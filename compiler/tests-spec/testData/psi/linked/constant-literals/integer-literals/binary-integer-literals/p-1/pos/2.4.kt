/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, binary-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] Digits may be separated by an underscore symbol, but no underscore can be placed before the first digit or after the last one.
 NUMBER: 4
 DESCRIPTION: Binary integer literals with underscore symbol before the first digit (it's considered as identifiers).
 */

val value = _____0b0_1_0_1_0_0
val value = _0B1_______0_______1_______0
val value = _0_0B1_0_1_0_1_0_0_1
val value = _0B000000000
val value = _0000000000b
val value = _0_9b
val value = ____________0b
val value = _0_b_0
val value = _b_0
val value = _b
val value = _b_
