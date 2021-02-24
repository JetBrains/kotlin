fun UInt.fUInt() {}
fun UByte.fUByte() {}
fun UShort.fUShort() {}
fun ULong.fULong() {}

fun test() {
    1.<!INAPPLICABLE_CANDIDATE!>fUInt<!>()
    1.<!INAPPLICABLE_CANDIDATE!>fUByte<!>()
    1.<!INAPPLICABLE_CANDIDATE!>fUShort<!>()
    1.<!INAPPLICABLE_CANDIDATE!>fULong<!>()

    3000000000 <!NONE_APPLICABLE!>until<!> 3000000004UL
    0 <!NONE_APPLICABLE!>until<!> 10u
}
