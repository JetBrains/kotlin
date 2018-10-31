/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 1
 * SENTENCE: [1] A real literal consists of the following parts: the whole-number part, the decimal point (ASCII period character .), the fraction part and the exponent.
 * NUMBER: 2
 * DESCRIPTION: Real literals with not allowed symbols as a separator of a whole-number part and a fraction part.
 * UNEXPECTED BEHAVIOUR
 */

val value = 0...1
val value = 1…1
val value = 000:1
val value = 2•0
val value = 00·0
val value = 300‚1
val value = 0000°1
val value = 1●1
val value = 8☺10

val value = 1. 2
val value = 1 . 2
val value = 1 .2
val value = 1	.2
val value = 1	.	2
val value = 1.	2
val value = 1
.2
val value = 1.
2
val value = 1.
2
val value = 1
.2
val value = 1.35
val value = 3.5