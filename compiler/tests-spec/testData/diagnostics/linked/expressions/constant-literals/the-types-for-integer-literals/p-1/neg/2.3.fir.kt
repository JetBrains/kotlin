// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    0 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    0 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    0 checkType { <!NONE_APPLICABLE!>check<!><Long>() }
}

// TESTCASE NUMBER: 2
fun case_2() {
    127 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    127 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    127 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(128)
    128 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    128 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    128 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    -128 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -128 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -128 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-129)
    -129 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -129 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -129 checkType { <!NONE_APPLICABLE!>check<!><Long>() }
}

// TESTCASE NUMBER: 3
fun case_3() {
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(32767)
    32767 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    32767 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    32767 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(32768)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(32768)
    32768 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    32768 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    32768 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-32768)
    -32768 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -32768 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -32768 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-32769)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(-32769)
    -32769 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -32769 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -32769 checkType { <!NONE_APPLICABLE!>check<!><Long>() }
}

// TESTCASE NUMBER: 4
fun case_4() {
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(2147483647)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(2147483647)
    2147483647 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    2147483647 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    2147483647 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(2147483648)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(2147483648)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(2147483648)
    2147483648 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    2147483648 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    2147483648 checkType { <!NONE_APPLICABLE!>check<!><Int>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-2147483648)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(-2147483648)
    -2147483648 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -2147483648 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -2147483648 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-2147483649)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(-2147483649)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(-2147483649)
    -2147483649 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -2147483649 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -2147483649 checkType { <!NONE_APPLICABLE!>check<!><Int>() }
}

// TESTCASE NUMBER: 5
fun case_5() {
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(9223372036854775807)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(9223372036854775807)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(9223372036854775807)
    9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Int>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-9223372036854775807)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(-9223372036854775807)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(-9223372036854775807)
    -9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Int>() }
}

// TESTCASE NUMBER: 6
fun case_6() {
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(<!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!>)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(<!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!>)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(<!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!>)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Long>(<!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!>)
    <!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Byte>() }
    <!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Short>() }
    <!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Int>() }
    <!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!NONE_APPLICABLE!>check<!><Long>() }
}
