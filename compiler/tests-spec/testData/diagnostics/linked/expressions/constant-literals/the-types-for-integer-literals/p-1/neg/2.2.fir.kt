// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    0L checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    1000000L checkType { <!NONE_APPLICABLE!>check<!><Int>() }
    0XAf10cDL checkType { <!NONE_APPLICABLE!>check<!><Int>() }
    0x0_0L checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    0b100_000_111_111L checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    0b0L checkType { <!NONE_APPLICABLE!>check<!><Byte>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(0L)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(1000000L)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(0XAf10cDL)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(0x0_0L)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(0b100_000_111_111L)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(0b0L)
}
