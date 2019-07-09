/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, decimal-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Sequences with decimal digit symbols separated by underscores.
 */

val value_1 = 1234_5678_90
val value_2 = 1_2_3_4_5_6_7_8_9_0
val value_3 = 1_2
val value_4 = 1_00000000000000000_1
val value_5 = 1_____________2

fun box(): String? {
    val value_6 = 9_____________0000
    val value_7 = 9____________0_0000
    val value_8 = 1_______________________________________________________________________________________________________________________________________________________0

    if (value_1 != 1234_5678_90 || value_1 != 1234567890) return null
    if (value_2 != 1_2_3_4_5_6_7_8_9_0 || value_2 != 1234567890) return null
    if (value_3 != 1_2 || value_3 != 12) return null
    if (value_4 != 1_00000000000000000_1 || value_4 != 1000000000000000001) return null
    if (value_5 != 1_____________2 || value_5 != 12) return null
    if (value_6 != 9_____________0000 || value_6 != 90000) return null
    if (value_7 != 9____________0_0000 || value_7 != 900000) return null
    if (value_8 != 1_______________________________________________________________________________________________________________________________________________________0 || value_8 != 10) return null

    return "OK"
}
