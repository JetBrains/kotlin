/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 2 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Real literals with a not allowed exponent mark with digits followed by a float suffix.
 */

val value = 0.0fe10
val value = 0.0F-e00
val value = 0.0fEe+000
val value = 0.0Fe+0000

val value = 00.0fe+0
val value = 000.00Fe00
val value = 0000.000fFEe-000

val value = 1.0Fe+1
val value = 22.00ffee22
val value = 333.000Fe-0
val value = 4444.0000fFe4444
val value = 55555.0Fee-55555
val value = 666666.00FeE+666666
