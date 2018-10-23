/*
 KOTLIN PSI SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] Any of the decimal, hexadecimal or binary literals may be suffixed by the long literal mark (symbol L).
 NUMBER: 2
 DESCRIPTION: Various integer literals with a long literal mark in not allowed positions.
 */

val value = 0x0123456L789abcdef
val value = 0lXal
val value = 0xL0L
val value = 0X4_______5_______d_______L7l
val value = 0xl_l
val value = 0b10101010101L0
val value = 0bL000000009l
val value = 0LB1___L____0____l___1____L___0
val value = 0Bl1______________0
val value = 0L0L
val value = 1L0L
