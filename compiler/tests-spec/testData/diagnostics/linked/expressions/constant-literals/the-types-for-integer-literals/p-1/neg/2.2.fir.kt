// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    0L checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    1000000L checkType { <!NONE_APPLICABLE!>check<!><Int>() }
    0XAf10cDL checkType { <!NONE_APPLICABLE!>check<!><Int>() }
    0x0_0L checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    0b100_000_111_111L checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    0b0L checkType { <!NONE_APPLICABLE!>check<!><Byte>() }

    checkSubtype<Short>(<!ARGUMENT_TYPE_MISMATCH!>0L<!>)
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>1000000L<!>)
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>0XAf10cDL<!>)
    checkSubtype<Byte>(<!ARGUMENT_TYPE_MISMATCH!>0x0_0L<!>)
    checkSubtype<Short>(<!ARGUMENT_TYPE_MISMATCH!>0b100_000_111_111L<!>)
    checkSubtype<Byte>(<!ARGUMENT_TYPE_MISMATCH!>0b0L<!>)
}
