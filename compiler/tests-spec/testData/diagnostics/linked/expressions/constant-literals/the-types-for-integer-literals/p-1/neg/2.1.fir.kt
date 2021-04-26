// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    0<!WRONG_LONG_SUFFIX!>l<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Long>() }
    10000000000000<!WRONG_LONG_SUFFIX!>l<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Long>() }
    0X000Af10cD<!WRONG_LONG_SUFFIX!>l<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Long>() }
    0x0_0<!WRONG_LONG_SUFFIX!>l<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Long>() }
    0b100_000_111_111<!WRONG_LONG_SUFFIX!>l<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Long>() }
    0b0<!WRONG_LONG_SUFFIX!>l<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Long>() }
}
