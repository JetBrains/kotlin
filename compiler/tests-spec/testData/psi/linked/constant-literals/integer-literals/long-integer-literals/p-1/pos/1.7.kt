/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SECTIONS: constant-literals, integer-literals, long-integer-literals
 * PARAGRAPH: 1
 * SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 * NUMBER: 7
 * DESCRIPTION: Binary prefix only suffixed by the long literal mark.
 */

val value = 0bL
val value = 0BL
