/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 2 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: Real literals suffixed by f/F (float suffix) with a not allowed exponent mark at the beginning.
 */

val value = E0f
val value = e000F

val value = E+0f
val value = e00f

val value = e+1F
val value = e22F
val value = E-333F
val value = e4444f
val value = e-55555f
val value = e666666F
val value = E7777777f
val value = e-88888888F
val value = E+999999999F
