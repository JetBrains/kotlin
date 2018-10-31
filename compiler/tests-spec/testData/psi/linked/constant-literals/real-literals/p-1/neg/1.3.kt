/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 1
 * SENTENCE: [1] A real literal consists of the following parts: the whole-number part, the decimal point (ASCII period character .), the fraction part and the exponent.
 * NUMBER: 3
 * DESCRIPTION: Real literals separeted by comments.
 */

val value = 0/* comment */.000001
val value = 9999/** some doc */.1

val value = 4/**//** some doc */.1

val value = 0/*
    ...
*/.000001
val value = 9999/**
 some doc
 */.1
val value = 9999// comment
.1
val value = 9999/***/
.1

val value = 1000/***/000.0
val value = 1000/*.*/000.0

val value = 4/** some/**/ doc */.1
val value = 4/* some/***/ doc */.19999
