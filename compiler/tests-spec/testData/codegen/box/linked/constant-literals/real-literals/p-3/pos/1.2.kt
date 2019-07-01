/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Real literals suffixed by f/F (float suffix) with omitted a whole-number part.
 */

val value_1 = .0F
val value_2 = .00F
val value_3 = .000F
val value_4 = .0000f

val value_5 = .1234567890f
val value_6 = .23456789f
val value_7 = .345678F
val value_8 = .4567f
val value_9 = .56F

fun box(): String? {
    val value_10 = .65F
    val value_11 = .7654f
    val value_12 = .876543f
    val value_13 = .98765432F
    val value_14 = .0987654321f

    val value_15 = .1111f
    val value_16 = .22222f
    val value_17 = .33333F
    val value_18 = .444444F
    val value_19 = .5555555F
    val value_20 = .66666666F
    val value_21 = .777777777F
    val value_22 = .8888888888f
    val value_23 = .99999999999f

    if (value_1.compareTo(.0F) != 0) return null
    if (value_2.compareTo(.00F) != 0 || value_2.compareTo(.0F) != 0) return null
    if (value_3.compareTo(.000F) != 0 || value_3.compareTo(.0f) != 0) return null
    if (value_4.compareTo(.0000f) != 0 || value_4.compareTo(.0f) != 0) return null

    if (value_5.compareTo(.1234567890f) != 0 || value_5.compareTo(.1234567890F) != 0) return null
    if (value_6.compareTo(.23456789F) != 0) return null
    if (value_7.compareTo(.345678F) != 0) return null
    if (value_8.compareTo(.4567f) != 0) return null
    if (value_9.compareTo(.56f) != 0) return null
    if (value_10.compareTo(.65F) != 0) return null
    if (value_11.compareTo(.7654f) != 0) return null
    if (value_12.compareTo(.876543F) != 0) return null
    if (value_13.compareTo(.98765432F) != 0) return null
    if (value_14.compareTo(.0987654321f) != 0) return null

    if (value_15.compareTo(.1111f) != 0) return null
    if (value_16.compareTo(.22222f) != 0) return null
    if (value_17.compareTo(.33333f) != 0) return null
    if (value_18.compareTo(.444444f) != 0) return null
    if (value_19.compareTo(.5555555F) != 0) return null
    if (value_20.compareTo(.66666666F) != 0) return null
    if (value_21.compareTo(.777777777f) != 0) return null
    if (value_22.compareTo(.8888888888F) != 0) return null
    if (value_23.compareTo(.99999999999f) != 0) return null

    return "OK"
}
