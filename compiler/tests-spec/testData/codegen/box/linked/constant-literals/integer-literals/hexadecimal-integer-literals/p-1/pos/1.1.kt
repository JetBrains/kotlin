/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Sequences with hexadecimal digit symbols.
 */

val value_1 = 0x1234567890
val value_2 = 0X23456789
val value_3 = 0x345678
val value_7 = 0X7654
val value_8 = 0x876543
val value_9 = 0x98765432

val value_10 = 0X0
val value_11 = 0x1

val value_12 = 0x100000
val value_13 = 0X1000001

val value_14 = 0X0000000
val value_16 = 0x00000010

val value_17 = 0xABCDEF
val value_18 = 0Xabcdef
val value_19 = 0xAbcD
val value_23 = 0XAAAAAAAA
val value_24 = 0xcDf
val value_25 = 0xAcccccccccA

val value_26 = 0x0123456789ABCDEF
val value_27 = 0x0123456789abcdef
val value_28 = 0XAA00AA
val value_29 = 0xBc12eF
val value_30 = 0xa0
val value_31 = 0XE1
val value_32 = 0xE1eE2eE3eE4e
val value_33 = 0XAAAAAA000000
val value_34 = 0xcDf091

fun box(): String? {
    val value_4 = 0X4567
    val value_5 = 0X56
    val value_6 = 0x65
    val value_15 = 0x0000001000000
    val value_20 = 0Xa
    val value_21 = 0xE
    val value_22 = 0xEeEeEeEe
    val value_35 = 0xAcccc0000cccccA
    val value_36 = 0X0000000A
    val value_37 = 0xe0000000
    val value_38 = 0x0000000D0000000
    val value_39 = 0xA0A

    if (value_1 != 0x1234567890 || value_1 != 78187493520) return null
    if (value_2 != 0X23456789 || value_2 != 591751049) return null
    if (value_3 != 0x345678 || value_3 != 3430008) return null
    if (value_4 != 0X4567 || value_4 != 17767) return null
    if (value_5 != 0X56 || value_5 != 86) return null
    if (value_6 != 0x65 || value_6 != 101) return null
    if (value_7 != 0X7654 || value_7 != 30292) return null
    if (value_8 != 0x876543 || value_8 != 8873283) return null
    if (value_9 != 0x98765432 || value_9 != 2557891634) return null
    if (value_10 != 0X0 || value_10 != 0) return null
    if (value_11 != 0x1 || value_11 != 1) return null
    if (value_12 != 0x100000 || value_12 != 1048576) return null
    if (value_13 != 0X1000001 || value_13 != 16777217) return null
    if (value_14 != 0X0000000 || value_14 != 0) return null
    if (value_15 != 0x0000001000000 || value_15 != 16777216) return null
    if (value_16 != 0x00000010 || value_16 != 16) return null
    if (value_17 != 0xABCDEF || value_17 != 11259375) return null
    if (value_18 != 0Xabcdef || value_18 != 11259375) return null
    if (value_19 != 0xAbcD || value_19 != 43981) return null
    if (value_20 != 0Xa || value_20 != 10) return null
    if (value_21 != 0xE || value_21 != 14) return null
    if (value_22 != 0xEeEeEeEe || value_22 != 4008636142) return null
    if (value_23 != 0XAAAAAAAA || value_23 != 2863311530) return null
    if (value_24 != 0xcDf || value_24 != 3295) return null
    if (value_25 != 0xAcccccccccA || value_25 != 11874725579978) return null
    if (value_26 != 0x0123456789ABCDEF || value_26 != 81985529216486895) return null
    if (value_27 != 0x0123456789abcdef || value_27 != 81985529216486895) return null
    if (value_28 != 0XAA00AA || value_28 != 11141290) return null
    if (value_29 != 0xBc12eF || value_29 != 12325615) return null
    if (value_30 != 0xa0 || value_30 != 160) return null
    if (value_31 != 0XE1 || value_31 != 225) return null
    if (value_32 != 0xE1eE2eE3eE4e || value_32 != 248413105155662) return null
    if (value_33 != 0XAAAAAA000000 || value_33 != 187649973288960) return null
    if (value_34 != 0xcDf091 || value_34 != 13496465) return null
    if (value_35 != 0xAcccc0000cccccA || value_35 != 778221136013741258) return null
    if (value_36 != 0X0000000A || value_36 != 10) return null
    if (value_37 != 0xe0000000 || value_37 != 3758096384) return null
    if (value_38 != 0x0000000D0000000 || value_38 != 3489660928) return null
    if (value_39 != 0xA0A || value_39 != 2570) return null

    return "OK"
}
