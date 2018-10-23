/*
 KOTLIN PSI SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, decimal-integer-literals
 PARAGRAPH: 1
 SENTENCE: [1] A sequence of decimal digit symbols (0 though 9) is a decimal integer literal.
 NUMBER: 1
 DESCRIPTION: Sequences with not all decimal digit symbols
 */

val value = 1234567890b
val value = 3456d78
val value = 45z67
val value = 5ffff6
val value = 6f5
val value = 7e6e54
val value = 876c54d3
val value = 0FF
val value = 0A

val value = 0$
val value = 2$100
val value = 2val value = 2^10
val value = 2\n
val value = 2@4
val value = 0#1
val value = 100!10
val value = 100&10
val value = 100|10
val value = 100)(10
val value = 100^10
val value = 100<10>
val value = 100\10
val value = 100,10
val value = 100:10
val value = 100::10
val value = 100'10
