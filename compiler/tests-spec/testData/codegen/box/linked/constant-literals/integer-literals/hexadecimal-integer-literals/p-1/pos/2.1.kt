/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, hexadecimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Sequences with hexadecimal digit symbols separated by underscores.
 */

val value_1 = 0x1_23a567b90
val value_2 = 0XF_______34a6789f
val value_3 = 0x3_c_c_c_7_8
val value_4 = 0X4_______b_______6_______d
val value_5 = 0X5__________________________________________________________________________________________________f
val value_6 = 0x0_______0

fun box(): String? {
    val value_7 = 0X0_0
    val value_8 = 0xa_______________________________________________________________________________________________________________________________________________________0
    val value_9 = 0x1_00000000000000_1

    if (value_1 != 0x123a567b90 || value_1 != 0x1_23a567b90 || value_1 != 78288157584) return null
    if (value_2 != 0XF34a6789f || value_2 != 0xF_______34a6789f || value_2 != 65307834527) return null
    if (value_3 != 0x3ccc78 || value_3 != 0x3_c_c_c_7_8 || value_3 != 3984504) return null
    if (value_4 != 0X4b6d || value_4 != 0x4_______b_______6_______d || value_4 != 19309) return null
    if (value_5 != 0X5f || value_5 != 0X5__________________________________________________________________________________________________f || value_5 != 95) return null
    if (value_6 != 0x0_______0 || value_6 != 0x00 || value_6 != 0x0 || value_6 != 0) return null
    if (value_7 != 0x0_0 || value_7 != 0X00 || value_7 != 0x0 || value_7 != 0) return null
    if (value_8 != 0xa0 || value_8 != 0xa_______________________________________________________________________________________________________________________________________________________0 || value_8 != 160) return null
    if (value_9 != 0x1000000000000001 || value_9 != 0X1_00000000000000_1 || value_9 != 1152921504606846977) return null

    return "OK"
}
