/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 1 -> sentence 1
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
