/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Various integer literals with underscores after a long literal mark.
 */

val value = 1000l___
val value = 90L_
val value = 0xFL_
val value = 0X0Al_
val value = 0b1L_
val value = 0B101001L_
