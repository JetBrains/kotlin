/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, decimal-integer-literals -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Integer literals with 0 in the first position and it contains more than 1 digit separated by underscore.
 */

val value = 0_0
val value = 000_000
val value = 0_10000
val value = 0_1000100
val value = 0_99999999
