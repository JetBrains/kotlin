// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    0L checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    1000000L checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Int>() }
    0XAf10cDL checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Int>() }
    0x0_0L checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    0b100_000_111_111L checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    0b0L checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(0L)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(1000000L)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(0XAf10cDL)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(0x0_0L)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(0b100_000_111_111L)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(0b0L)
}
