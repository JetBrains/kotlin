/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Simple real literals with omitted a whole-number part.
 */

val value_1 = .0
val value_2 = .00
val value_3 = .000
val value_4 = .0000

val value_5 = .1234567890
val value_6 = .23456789
val value_7 = .345678
val value_8 = .4567
val value_9 = .56

fun box(): String? {
    val value_10 = .65
    val value_11 = .7654
    val value_12 = .876543
    val value_13 = .98765432
    val value_14 = .0987654321

    val value_15 = .1111
    val value_16 = .22222
    val value_17 = .33333
    val value_18 = .444444
    val value_19 = .5555555
    val value_20 = .66666666
    val value_21 = .777777777
    val value_22 = .8888888888
    val value_23 = .99999999999

    if (value_1.compareTo(.0) != 0) return null
    if (value_2.compareTo(.00) != 0 || value_2.compareTo(.0) != 0) return null
    if (value_3.compareTo(.000) != 0 || value_3.compareTo(.0) != 0) return null
    if (value_4.compareTo(.0000) != 0 || value_4.compareTo(.0) != 0) return null

    if (value_5.compareTo(.1234567890) != 0 || value_5.compareTo(.1234567890) != 0) return null
    if (value_6.compareTo(.23456789) != 0) return null
    if (value_7.compareTo(.345678) != 0) return null
    if (value_8.compareTo(.4567) != 0) return null
    if (value_9.compareTo(.56) != 0) return null
    if (value_10.compareTo(.65) != 0) return null
    if (value_11.compareTo(.7654) != 0) return null
    if (value_12.compareTo(.876543) != 0) return null
    if (value_13.compareTo(.98765432) != 0) return null
    if (value_14.compareTo(.0987654321) != 0) return null

    if (value_15.compareTo(.1111) != 0) return null
    if (value_16.compareTo(.22222) != 0) return null
    if (value_17.compareTo(.33333) != 0) return null
    if (value_18.compareTo(.444444) != 0) return null
    if (value_19.compareTo(.5555555) != 0) return null
    if (value_20.compareTo(.66666666) != 0) return null
    if (value_21.compareTo(.777777777) != 0) return null
    if (value_22.compareTo(.8888888888) != 0) return null
    if (value_23.compareTo(.99999999999) != 0) return null

    return "OK"
}
