// FIR_IDENTICAL
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, the-types-for-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Type checking of decimal integer literals.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case_1() {
    0 checkType { check<Int>() }
    checkSubtype<Int>(0)
    checkSubtype<Short>(0)
    checkSubtype<Byte>(0)
    checkSubtype<Long>(0)
}

// TESTCASE NUMBER: 2
fun case_2() {
    127 checkType { check<Int>() }
    checkSubtype<Int>(127)
    checkSubtype<Short>(127)
    checkSubtype<Byte>(127)
    checkSubtype<Long>(127)

    128 checkType { check<Int>() }
    checkSubtype<Int>(128)
    checkSubtype<Short>(128)
    checkSubtype<Long>(128)

    -128 checkType { check<Int>() }
    checkSubtype<Int>(-128)
    checkSubtype<Short>(-128)
    checkSubtype<Byte>(-128)
    checkSubtype<Long>(-128)

    -129 checkType { check<Int>() }
    checkSubtype<Int>(-129)
    checkSubtype<Short>(-129)
    checkSubtype<Long>(-129)
}

// TESTCASE NUMBER: 3
fun case_3() {
    32767 checkType { check<Int>() }
    checkSubtype<Int>(32767)
    checkSubtype<Short>(32767)
    checkSubtype<Long>(32767)

    32768 checkType { check<Int>() }
    checkSubtype<Int>(32768)
    checkSubtype<Long>(32768)

    -32768 checkType { check<Int>() }
    checkSubtype<Int>(-32768)
    checkSubtype<Short>(-32768)
    checkSubtype<Long>(-32768)

    -32769 checkType { check<Int>() }
    checkSubtype<Int>(-32769)
    checkSubtype<Long>(-32769)
}

// TESTCASE NUMBER: 4
fun case_4() {
    2147483647 checkType { check<Int>() }
    checkSubtype<Int>(2147483647)
    checkSubtype<Long>(2147483647)

    2147483648 checkType { check<Long>() }
    checkSubtype<Long>(2147483648)

    -2147483648 checkType { check<Int>() }
    checkSubtype<Int>(-2147483648)
    checkSubtype<Long>(-2147483648)

    -2147483649 checkType { check<Long>() }
    checkSubtype<Long>(-2147483649)
}

// TESTCASE NUMBER: 5
fun case_5() {
    9223372036854775807 checkType { check<Long>() }
    checkSubtype<Long>(9223372036854775807)

    -9223372036854775807 checkType { check<Long>() }
    checkSubtype<Long>(-9223372036854775807)
}
