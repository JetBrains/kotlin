/*
 KOTLIN PSI SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 NUMBER: 10
 DESCRIPTION: Long literal mark in not allowed positions (it's considered as identifiers).
 */

val value = l1234
val value = L0xA0Al
val value = _l
val value = _L
