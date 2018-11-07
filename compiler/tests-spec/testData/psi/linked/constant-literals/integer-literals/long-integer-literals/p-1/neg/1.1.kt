/*
 KOTLIN PSI SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 NUMBER: 1
 DESCRIPTION: Various integer literals with a long literal mark doublicate.
 */

val value = 1234567890lL
val value = 1Ll
val value = 1_ll
val value = 1234_5678_90LLLLLL
val value = 0x1llllll
val value = 0B0LlLlLl
