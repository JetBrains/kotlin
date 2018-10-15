/*
 KOTLIN PSI SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 NUMBER: 3
 DESCRIPTION: Various integer literals with underscores after a long literal mark.
 */

val value = 1000l___
val value = 90L_
val value = 0xFL_
val value = 0X0Al_
val value = 0b1L_
val value = 0B101001L_
