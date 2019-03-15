// !CHECK_TYPE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Type checking (comparison with invalid types) of various integer literals.
 */

// TESTCASE NUMBER: 1
fun case_1() {
    0 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    0 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    0 checkType { <!TYPE_MISMATCH!>_<!><Long>() }
}

// TESTCASE NUMBER: 2
fun case_2() {
    127 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    127 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    127 checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>128<!>)
    128 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    128 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    128 checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    -128 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    -128 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    -128 checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-129<!>)
    -129 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    -129 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    -129 checkType { <!TYPE_MISMATCH!>_<!><Long>() }
}

// TESTCASE NUMBER: 3
fun case_3() {
    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>32767<!>)
    32767 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    32767 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    32767 checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>32768<!>)
    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>32768<!>)
    32768 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    32768 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    32768 checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-32768<!>)
    -32768 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    -32768 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    -32768 checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-32769<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-32769<!>)
    -32769 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    -32769 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    -32769 checkType { <!TYPE_MISMATCH!>_<!><Long>() }
}

// TESTCASE NUMBER: 4
fun case_4() {
    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483647<!>)
    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483647<!>)
    2147483647 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    2147483647 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    2147483647 checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483648<!>)
    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483648<!>)
    checkSubtype<Int>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>2147483648<!>)
    2147483648 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    2147483648 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    2147483648 checkType { <!TYPE_MISMATCH!>_<!><Int>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-2147483648<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-2147483648<!>)
    -2147483648 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    -2147483648 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    -2147483648 checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-2147483649<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-2147483649<!>)
    checkSubtype<Int>(<!TYPE_MISMATCH!>-2147483649<!>)
    -2147483649 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    -2147483649 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    -2147483649 checkType { <!TYPE_MISMATCH!>_<!><Int>() }
}

// TESTCASE NUMBER: 5
fun case_5() {
    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>9223372036854775807<!>)
    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>9223372036854775807<!>)
    checkSubtype<Int>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>9223372036854775807<!>)
    9223372036854775807 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    9223372036854775807 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    9223372036854775807 checkType { <!TYPE_MISMATCH!>_<!><Int>() }

    checkSubtype<Byte>(<!TYPE_MISMATCH!>-9223372036854775807<!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-9223372036854775807<!>)
    checkSubtype<Int>(<!TYPE_MISMATCH!>-9223372036854775807<!>)
    -9223372036854775807 checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    -9223372036854775807 checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    -9223372036854775807 checkType { <!TYPE_MISMATCH!>_<!><Int>() }
}

// TESTCASE NUMBER: 6
fun case_6() {
    checkSubtype<Byte>(<!TYPE_MISMATCH!>-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!><!>)
    checkSubtype<Short>(<!TYPE_MISMATCH!>-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!><!>)
    checkSubtype<Int>(-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!>)
    checkSubtype<Long>(<!TYPE_MISMATCH!>-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!><!>)
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!> checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!> checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!> checkType { _<Int>() }
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000<!> checkType { <!TYPE_MISMATCH!>_<!><Long>() }
}
