/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Various integer literals with a long literal mark doublicate.
 */

val value = 1234567890lL
val value = 1Ll
val value = 1_ll
val value = 1234_5678_90LLLLLL
val value = 0x1llllll
val value = 0B0LlLlLl
