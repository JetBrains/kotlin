// !DIAGNOSTICS: -UNUSED_VARIABLE

fun test(
    ub1: UByte, ub2: UByte,
    us1: UShort, us2: UShort,
    ui1: UInt, ui2: UInt,
    ul1: ULong, ul2: ULong
) {
    val ub = <!FORBIDDEN_IDENTITY_EQUALS!>ub1 === ub2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>ub1 !== ub2<!>
    val us = <!FORBIDDEN_IDENTITY_EQUALS!>us1 === us2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>us1 !== us2<!>
    val ui = <!FORBIDDEN_IDENTITY_EQUALS!>ui1 === ui2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>ui1 !== ui2<!>
    val ul = <!FORBIDDEN_IDENTITY_EQUALS!>ul1 === ul2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>ul1 !== ul2<!>

    val u = <!FORBIDDEN_IDENTITY_EQUALS!>ub1 === ul1<!>

    val a1 = <!FORBIDDEN_IDENTITY_EQUALS!>1u === 2u<!> || <!FORBIDDEN_IDENTITY_EQUALS!>1u !== 2u<!>
    val a2 = <!FORBIDDEN_IDENTITY_EQUALS!>0xFFFF_FFFF_FFFF_FFFFu === 0xFFFF_FFFF_FFFF_FFFFu<!>

    val bu1 = 1u
    val bu2 = 1u

    val c1 = <!FORBIDDEN_IDENTITY_EQUALS!>bu1 === bu2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>bu1 !== bu2<!>
}
