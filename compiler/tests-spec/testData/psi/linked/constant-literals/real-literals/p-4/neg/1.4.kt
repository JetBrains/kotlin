/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 4 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: Real literals with omitted whole-number part, float suffix not at the end and underscores at the different positions.
 */

val value = 1F0_0
val value = 22f1_01923___091290_4
val value = 333e-0_0000000000F1_2903490
val value = 4_4_4_4E-9_9_9_9_9_9_9_9_9_9_9_9_9_9_9_9_9f0_0_0_0_0_0_0_0_0_0_0_0_0_0_0_0
val value = 777777_______________7e0_909090_9_0_9_0F0
val value = 8888_______________8888e-1f1_______________0
val value = 99______999______99______99EF0

val value = 123______4567______9e9876______54321F9______9999______99999______999999______99999
val value = 2__3__4__5__6__7__8E0f0
val value = 5e50__501f01__1
val value = 6__54e5F10
val value = 765__43f00__000
val value = 8__765__432F010
val value = 9__8765__4321f1__00
