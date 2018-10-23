/*
 KOTLIN PSI SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 NUMBER: 4
 DESCRIPTION: The long literal mark after not integer literals.
 */

val value = 'a'l
val value = .09L
val value = 10.10l
val value = "..."L
