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
    0 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    0 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    0 checkType { <!NONE_APPLICABLE!>check<!><Long>() }
}

// TESTCASE NUMBER: 2
fun case_2() {
    127 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    127 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    127 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>128<!>)
    128 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    128 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    128 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    -128 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -128 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -128 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-129<!>)
    -129 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -129 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -129 checkType { <!NONE_APPLICABLE!>check<!><Long>() }
}

// TESTCASE NUMBER: 3
fun case_3() {
    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>32767<!>)
    32767 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    32767 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    32767 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>32768<!>)
    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>32768<!>)
    32768 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    32768 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    32768 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-32768<!>)
    -32768 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -32768 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -32768 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-32769<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-32769<!>)
    -32769 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -32769 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -32769 checkType { <!NONE_APPLICABLE!>check<!><Long>() }
}

// TESTCASE NUMBER: 4
fun case_4() {
    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483647<!>)
    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483647<!>)
    2147483647 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    2147483647 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    2147483647 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483648<!>)
    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483648<!>)
    checkSubtype<Int>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483648<!>)
    2147483648 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    2147483648 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    2147483648 checkType { <!NONE_APPLICABLE!>check<!><Int>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-2147483648<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-2147483648<!>)
    -2147483648 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -2147483648 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -2147483648 checkType { <!NONE_APPLICABLE!>check<!><Long>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-2147483649<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-2147483649<!>)
    checkSubtype<Int>(<!TYPE_MISMATCH!>-2147483649<!>)
    -2147483649 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -2147483649 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -2147483649 checkType { <!NONE_APPLICABLE!>check<!><Int>() }
}

// TESTCASE NUMBER: 5
fun case_5() {
    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>9223372036854775807<!>)
    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>9223372036854775807<!>)
    checkSubtype<Int>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>9223372036854775807<!>)
    9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Int>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-9223372036854775807<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-9223372036854775807<!>)
    checkSubtype<Int>(<!TYPE_MISMATCH!>-9223372036854775807<!>)
    -9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -9223372036854775807 checkType { <!NONE_APPLICABLE!>check<!><Int>() }
}

// TESTCASE NUMBER: 6
fun case_6() {
    checkSubtype<Byte>(<!TYPE_MISMATCH!>-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!><!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!><!>)
    checkSubtype<Int>(-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!>)
    checkSubtype<Long>(<!TYPE_MISMATCH!>-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!><!>)
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!> checkType { <!NONE_APPLICABLE!>check<!><Byte>() }
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!> checkType { <!NONE_APPLICABLE!>check<!><Short>() }
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!> checkType { check<Int>() }
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!> checkType { <!NONE_APPLICABLE!>check<!><Long>() }
}
