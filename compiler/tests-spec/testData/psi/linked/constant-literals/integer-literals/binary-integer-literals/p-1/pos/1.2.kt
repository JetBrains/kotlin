/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Binary integer literals with invalid [2-9] digit symbols.
 */

val value = 0B000123412
val value = 0b000000009
val value = 0b200000000
val value = 0b1234567890
