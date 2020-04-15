// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, constant-literals, the-types-for-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Type checking (comparison with invalid types) of various integer literals.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case_1() {
    0 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    0 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    0 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }
}

// TESTCASE NUMBER: 2
fun case_2() {
    127 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    127 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    127 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(128)
    128 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    128 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    128 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }

    -128 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    -128 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    -128 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-129)
    -129 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    -129 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    -129 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }
}

// TESTCASE NUMBER: 3
fun case_3() {
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(32767)
    32767 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    32767 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    32767 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(32768)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(32768)
    32768 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    32768 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    32768 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-32768)
    -32768 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    -32768 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    -32768 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-32769)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(-32769)
    -32769 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    -32769 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    -32769 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }
}

// TESTCASE NUMBER: 4
fun case_4() {
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(2147483647)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(2147483647)
    2147483647 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    2147483647 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    2147483647 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(2147483648)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(2147483648)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(2147483648)
    2147483648 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    2147483648 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    2147483648 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Int>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-2147483648)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(-2147483648)
    -2147483648 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    -2147483648 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    -2147483648 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-2147483649)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(-2147483649)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(-2147483649)
    -2147483649 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    -2147483649 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    -2147483649 checkType { check<Int>() }
}

// TESTCASE NUMBER: 5
fun case_5() {
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(9223372036854775807)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(9223372036854775807)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(9223372036854775807)
    9223372036854775807 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    9223372036854775807 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    9223372036854775807 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Int>() }

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(-9223372036854775807)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(-9223372036854775807)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(-9223372036854775807)
    -9223372036854775807 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    -9223372036854775807 checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    -9223372036854775807 checkType { check<Int>() }
}

// TESTCASE NUMBER: 6
fun case_6() {
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Byte>(<!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!>)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Short>(<!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!>)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(<!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!>)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Long>(<!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!>)
    <!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>check<!><Byte>() }
    <!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>check<!><Short>() }
    <!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>check<!><Int>() }
    <!AMBIGUITY!>-<!><!ILLEGAL_CONST_EXPRESSION!>100000000000000000000000000000000<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>check<!><Long>() }
}
