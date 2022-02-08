// !DIAGNOSTICS: -UNUSED_VARIABLE

fun test(
    ub1: UByte, ub2: UByte,
    us1: UShort, us2: UShort,
    ui1: UInt, ui2: UInt,
    ul1: ULong, ul2: ULong
) {
    val ub = ub1 === ub2 || ub1 !== ub2
    val us = us1 === us2 || us1 !== us2
    val ui = ui1 === ui2 || ui1 !== ui2
    val ul = ul1 === ul2 || ul1 !== ul2

    val u = <!EQUALITY_NOT_APPLICABLE!>ub1 === ul1<!>

    val a1 = 1u === 2u || 1u !== 2u
    val a2 = 0xFFFF_FFFF_FFFF_FFFFu === 0xFFFF_FFFF_FFFF_FFFFu

    val bu1 = 1u
    val bu2 = 1u

    val c1 = bu1 === bu2 || bu1 !== bu2
}
