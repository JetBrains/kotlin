/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 1 -> sentence 3
 * NUMBER: 3
 * DESCRIPTION: Real literals with a float suffix and not allowed symbols as a separator of a whole-number part and a fraction part.
 * UNEXPECTED BEHAVIOUR
 */

val value = 0...1f
val value = 1…1f
val value = 000:1F
val value = 2•0F
val value = 00·0f
val value = 300‚1F
val value = 0000°1f
val value = 1●1F
val value = 8☺10f

val value = 1. 2f
val value = 1 . 2f
val value = 1 .2F
val value = 1	.2f
val value = 1	.	2F
val value = 1.	2f
val value = 1
.2F
val value = 1.
    2F
val value = 1.
    2f
val value = 1
.2f
val value = 1.35f
val value = 3.5F