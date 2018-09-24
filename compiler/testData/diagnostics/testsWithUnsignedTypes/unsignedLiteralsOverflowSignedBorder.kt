// !DIAGNOSTICS: -UNUSED_PARAMETER

const val u1: UByte = 0xFFu
const val u2: UShort = 0xFFFFu
const val u3: UInt = 0xFFFF_FFFFu
const val u4: ULong = 0xFFFF_FFFF_FFFF_FFFFu
const val u5: ULong = 18446744073709551615u

const val u6 = 0xFFFF_FFFF_FFFF_FFFFu
const val u7 = 18446744073709551615u

val u8: Comparable<*> = 0xFFFF_FFFF_FFFF_FFFFu

const val u9 = 0xFFFF_FFFF_FFFF_FFFFUL

fun takeUByte(ubyte: UByte) {}

fun test() {
    takeUByte(200u)
    takeUByte(255u)
    takeUByte(0xFFu)
}

val s1: UByte = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>256u<!>
val s2 = <!INT_LITERAL_OUT_OF_RANGE!>18446744073709551616u<!>