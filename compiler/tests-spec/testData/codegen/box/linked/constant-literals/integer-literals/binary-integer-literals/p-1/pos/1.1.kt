/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Sequences with binary digit symbols.
 */

val value_1 = 0b1110001100
val value_2 = 0B11000110
val value_3 = 0b100011
val value_7 = 0B1000
val value_8 = 0b110001
val value_9 = 0b01100011

val value_10 = 0B0
val value_11 = 0b1

val value_12 = 0b100000
val value_13 = 0B1000001

val value_14 = 0B0000000
val value_16 = 0b00000010

val value_17 = 0b001101
val value_18 = 0B001101
val value_19 = 0b0011
val value_23 = 0B00000000
val value_24 = 0b111
val value_25 = 0b01111111110

val value_26 = 0b0111000110001101
val value_27 = 0b0111000110001101
val value_28 = 0B000000
val value_29 = 0b011101
val value_30 = 0b00
val value_31 = 0B01
val value_32 = 0b010010010000
val value_33 = 0B000000000000
val value_34 = 0b111001

fun box(): String? {
    val value_4 = 0B0001
    val value_5 = 0B00
    val value_6 = 0b00
    val value_15 = 0b0000001000000
    val value_20 = 0B0
    val value_21 = 0b0
    val value_22 = 0b00000000
    val value_35 = 0b011110000111110
    val value_36 = 0B00000000
    val value_37 = 0b00000000
    val value_38 = 0b000000010000000
    val value_39 = 0b000

    if (value_1 != 0b1110001100 || value_1 != 908) return null
    if (value_2 != 0B11000110 || value_2 != 198) return null
    if (value_3 != 0b100011 || value_3 != 35) return null
    if (value_4 != 0B0001 || value_4 != 1) return null
    if (value_5 != 0B00 || value_5 != 0) return null
    if (value_6 != 0b00 || value_6 != 0) return null
    if (value_7 != 0B1000 || value_7 != 8) return null
    if (value_8 != 0b110001 || value_8 != 49) return null
    if (value_9 != 0b01100011 || value_9 != 99) return null
    if (value_10 != 0B0 || value_10 != 0) return null
    if (value_11 != 0b1 || value_11 != 1) return null
    if (value_12 != 0b100000 || value_12 != 32) return null
    if (value_13 != 0B1000001 || value_13 != 65) return null
    if (value_14 != 0B0000000 || value_14 != 0) return null
    if (value_15 != 0b0000001000000 || value_15 != 64) return null
    if (value_16 != 0b00000010 || value_16 != 2) return null
    if (value_17 != 0b001101 || value_17 != 13) return null
    if (value_18 != 0B001101 || value_18 != 13) return null
    if (value_19 != 0b0011 || value_19 != 3) return null
    if (value_20 != 0B0 || value_20 != 0) return null
    if (value_21 != 0b0 || value_21 != 0) return null
    if (value_22 != 0b00000000 || value_22 != 0) return null
    if (value_23 != 0B00000000 || value_23 != 0) return null
    if (value_24 != 0b111 || value_24 != 7) return null
    if (value_25 != 0b01111111110 || value_25 != 1022) return null
    if (value_26 != 0b0111000110001101 || value_26 != 29069) return null
    if (value_27 != 0b0111000110001101 || value_27 != 29069) return null
    if (value_28 != 0B000000 || value_28 != 0) return null
    if (value_29 != 0b011101 || value_29 != 29) return null
    if (value_30 != 0b00 || value_30 != 0) return null
    if (value_31 != 0B01 || value_31 != 1) return null
    if (value_32 != 0b010010010000 || value_32 != 1168) return null
    if (value_33 != 0B000000000000 || value_33 != 0) return null
    if (value_34 != 0b111001 || value_34 != 57) return null
    if (value_35 != 0b011110000111110 || value_35 != 15422) return null
    if (value_36 != 0B00000000 || value_36 != 0) return null
    if (value_37 != 0b00000000 || value_37 != 0) return null
    if (value_38 != 0b000000010000000 || value_38 != 128) return null
    if (value_39 != 0b000 || value_39 != 0) return null

    return "OK"
}
