/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Decimal integer literals with underscores suffixed by the long literal mark.
 */

val value = 1234_5678_90L
val value = 1_2_3_4_5_6_7_8_9_0L
val value = 1_2L
val value = 1_00000000000000000_1L
val value = 1_____________2L
val value = 9_____________0000L
val value = 9____________0_0000L
val value = 1_______________________________________________________________________________________________________________________________________________________0L

val value = 1_L
val value = 1_00000000000000000_L
val value = 1_____________L
val value = 9____________0_L
val value = 1_______________________________________________________________________________________________________________________________________________________L
