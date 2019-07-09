/*
 * KOTLIN PSI SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Hexadecimal integer literals with not allowed symbols.
 */

val value = 0x876L543
val value = 0xf876L543
val value = 0xx1234567890
val value = 0Xx23456789
val value = 0xX345678
val value = 0X45x67
val value = 0X50X6
val value = 0x60x5
val value = 0xXx7654
val value = 0XG
val value = 0xF1z
val value = 0x100M000
val value = 0XXXX1000001
val value = 0x00000010x
val value = 0xABCDEFXX
val value = 0Xabcdefghijklmnopqrstuvwxyz
val value = 0XABCDEFGHIJKLMNOPQRSTUVWXYZ
val value = 0Xа
val value = 0x10С10
val value = 0xeeeeеееее
val value = 0xxxxxxx
val value = 0X0XXXXXX
val value = 0X0x0

val value = 0x$
val value = 0x4$0x100
val value = 0x1val value = 2x^0x10
val value = 0X\n
val value = 0x@0x4
val value = 0x#0x1
val value = 0x!0X10
val value = 0X&0X10
val value = 0X|0X10
val value = 0X)(0X10
val value = 0x^0x10
val value = 0x<0X10>
val value = 0x\0X10
val value = 0X,0X10
val value = 0X:0x10
val value = 0X::0x10
val value = 0X'0x10
