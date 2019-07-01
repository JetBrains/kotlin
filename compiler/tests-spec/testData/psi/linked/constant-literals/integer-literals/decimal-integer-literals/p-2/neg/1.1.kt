/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, decimal-integer-literals -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Integer literals with 0 in the first position and it contains more than 1 digit.
 */

val value = 00
val value = 000000
val value = 010000
val value = 01000100
val value = 099999999
