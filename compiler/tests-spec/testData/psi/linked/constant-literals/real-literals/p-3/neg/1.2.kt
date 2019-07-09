/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Real literals with dots at the beginning and exponent mark/float suffix right after it.
 */

val value = .f
val value = ..F
val value = .e10
val value = .+e1

val value = ..-E10F
val value = ...+e000000F
val value = ..e1f1
val value = ...E0000000000
