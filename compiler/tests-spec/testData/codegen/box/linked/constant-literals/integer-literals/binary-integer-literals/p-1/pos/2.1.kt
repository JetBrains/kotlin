/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Sequences with binary digit symbols separated by underscores.
 */

val value_1 = 0b1_110110100
val value_2 = 0b1_______1011010
val value_3 = 0b1_0_1_1_0_1
val value_4 = 0b0_______1_______1_______0
val value_5 = 0b1__________________________________________________________________________________________________1
val value_6 = 0b0_______0

fun box(): String? {
    val value_7 = 0B0_0
    val value_8 = 0b1_______________________________________________________________________________________________________________________________________________________0
    val value_9 = 0b1_00000000000000_1

    if (value_1 != 0b1110110100 || value_1 != 0B1_110110100 || value_1 != 948) return null
    if (value_2 != 0b11011010 || value_2 != 0b1_______1011010 || value_2 != 218) return null
    if (value_3 != 0b101101 || value_3 != 0b1_0_1_1_0_1 || value_3 != 45) return null
    if (value_4 != 0b0110 || value_4 != 0B0_______1_______1_______0 || value_4 != 6) return null
    if (value_5 != 0b11 || value_5 != 0b1__________________________________________________________________________________________________1 || value_5 != 3) return null
    if (value_6 != 0b0_______0 || value_6 != 0b00 || value_6 != 0b0 || value_6 != 0) return null
    if (value_7 != 0b0_0 || value_7 != 0b00 || value_7 != 0b0 || value_7 != 0) return null
    if (value_8 != 0b10 || value_8 != 0B1_______________________________________________________________________________________________________________________________________________________0 || value_8 != 2) return null
    if (value_9 != 0b1000000000000001 || value_9 != 0B1_00000000000000_1 || value_9 != 32769) return null

    return "OK"
}
