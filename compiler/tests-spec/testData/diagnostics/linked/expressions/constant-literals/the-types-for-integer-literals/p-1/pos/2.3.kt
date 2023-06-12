// FIR_IDENTICAL
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, the-types-for-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Type checking of hexadecimal integer literals.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case_1() {
    0x0 checkType { check<Int>() }
    checkSubtype<Int>(0x0)
    checkSubtype<Short>(0x0)
    checkSubtype<Byte>(0x0)
    checkSubtype<Long>(0x0)
}

// TESTCASE NUMBER: 2
fun case_2() {
    0x7F checkType { check<Int>() }
    checkSubtype<Int>(0x7F)
    checkSubtype<Short>(0x7F)
    checkSubtype<Byte>(0x7F)
    checkSubtype<Long>(0x7F)

    0X80 checkType { check<Int>() }
    checkSubtype<Int>(0X80)
    checkSubtype<Short>(0X80)
    checkSubtype<Long>(0X80)

    -0X80 checkType { check<Int>() }
    checkSubtype<Int>(-0X80)
    checkSubtype<Short>(-0X80)
    checkSubtype<Byte>(-0X80)
    checkSubtype<Long>(-0X80)

    -0x81 checkType { check<Int>() }
    checkSubtype<Int>(-0x81)
    checkSubtype<Short>(-0x81)
    checkSubtype<Long>(-0x81)
}

// TESTCASE NUMBER: 3
fun case_3() {
    0x7FFF checkType { check<Int>() }
    checkSubtype<Int>(0x7FFF)
    checkSubtype<Short>(0x7FFF)
    checkSubtype<Long>(0x7FFF)

    0x8000 checkType { check<Int>() }
    checkSubtype<Int>(0x8000)
    checkSubtype<Long>(0x8000)

    -0x8000 checkType { check<Int>() }
    checkSubtype<Int>(-0x8000)
    checkSubtype<Short>(-0x8000)
    checkSubtype<Long>(-0x8000)

    -0X8001 checkType { check<Int>() }
    checkSubtype<Int>(-0X8001)
    checkSubtype<Long>(-0X8001)
}

// TESTCASE NUMBER: 4
fun case_4() {
    0x7FFFFFFF checkType { check<Int>() }
    checkSubtype<Int>(0x7FFFFFFF)
    checkSubtype<Long>(0x7FFFFFFF)

    0x80000000 checkType { check<Long>() }
    checkSubtype<Long>(0x80000000)

    -0x80000000 checkType { check<Int>() }
    checkSubtype<Int>(-0x80000000)
    checkSubtype<Long>(-0x80000000)

    -0x80000001 checkType { check<Long>() }
    checkSubtype<Long>(-0x80000001)
}

// TESTCASE NUMBER: 5
fun case_5() {
    0X7FFFFFFFFFFFFFFF checkType { check<Long>() }
    checkSubtype<Long>(0X7FFFFFFFFFFFFFFF)

    -0X7FFFFFFFFFFFFFFF checkType { check<Long>() }
    checkSubtype<Long>(-0X7FFFFFFFFFFFFFFF)
}
