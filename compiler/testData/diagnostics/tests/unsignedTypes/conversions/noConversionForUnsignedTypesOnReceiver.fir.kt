fun UInt.fUInt() {}
fun UByte.fUByte() {}
fun UShort.fUShort() {}
fun ULong.fULong() {}

fun test() {
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fUInt<!>()
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fUByte<!>()
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fUShort<!>()
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fULong<!>()

    3000000000 <!NONE_APPLICABLE!>until<!> 3000000004UL
    0 <!NONE_APPLICABLE!>until<!> 10u
}
