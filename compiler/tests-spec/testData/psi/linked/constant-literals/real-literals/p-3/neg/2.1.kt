/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Real literals with omitted fraction part and digits followed by a float suffix.
 */

val value = 1F0
val value = 22f1019230912904
val value = 333e-00000000000F12903490
val value = 4444E-99999999999999999f000000000000000000
val value = 7777777e09090909090F0
val value = 88888888e-1f1
val value = 999999999EF0

val value = 123456789e987654321F999999999999999999999
val value = 2345678E0f0
val value = 5e50501f011
val value = 654e5F10
val value = 76543f00000
val value = 8765432F010
val value = 987654321f100
