// FIR_IDENTICAL
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, constant-literals, the-types-for-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 4
 * DESCRIPTION: Type checking of binary integer literals.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case_1() {
    0b0 checkType { check<Int>() }
    checkSubtype<Int>(0b0)
    checkSubtype<Short>(0b0)
    checkSubtype<Byte>(0b0)
    checkSubtype<Long>(0b0)
}

// TESTCASE NUMBER: 2
fun case_2() {
    0B1111111 checkType { check<Int>() }
    checkSubtype<Int>(0B1111111)
    checkSubtype<Short>(0B1111111)
    checkSubtype<Byte>(0B1111111)
    checkSubtype<Long>(0B1111111)

    0b10000000 checkType { check<Int>() }
    checkSubtype<Int>(0b10000000)
    checkSubtype<Short>(0b10000000)
    checkSubtype<Long>(0b10000000)

    -0B10000000 checkType { check<Int>() }
    checkSubtype<Int>(-0B10000000)
    checkSubtype<Short>(-0B10000000)
    checkSubtype<Byte>(-0B10000000)
    checkSubtype<Long>(-0B10000000)

    -0b10000001 checkType { check<Int>() }
    checkSubtype<Int>(-0b10000001)
    checkSubtype<Short>(-0b10000001)
    checkSubtype<Long>(-0b10000001)
}

// TESTCASE NUMBER: 3
fun case_3() {
    0B111111111111111 checkType { check<Int>() }
    checkSubtype<Int>(0B111111111111111)
    checkSubtype<Short>(0B111111111111111)
    checkSubtype<Long>(0B111111111111111)

    0b1000000000000000 checkType { check<Int>() }
    checkSubtype<Int>(0b1000000000000000)
    checkSubtype<Long>(0b1000000000000000)

    -0b1000000000000000 checkType { check<Int>() }
    checkSubtype<Int>(-0b1000000000000000)
    checkSubtype<Short>(-0b1000000000000000)
    checkSubtype<Long>(-0b1000000000000000)

    -0B1000000000000001 checkType { check<Int>() }
    checkSubtype<Int>(-0B1000000000000001)
    checkSubtype<Long>(-0B1000000000000001)
}

// TESTCASE NUMBER: 4
fun case_4() {
    0b1111111111111111111111111111111 checkType { check<Int>() }
    checkSubtype<Int>(0b1111111111111111111111111111111)
    checkSubtype<Long>(0b1111111111111111111111111111111)

    0B10000000000000000000000000000000 checkType { check<Long>() }
    checkSubtype<Long>(0B10000000000000000000000000000000)

    -0B10000000000000000000000000000000 checkType { check<Int>() }
    checkSubtype<Int>(-0B10000000000000000000000000000000)
    checkSubtype<Long>(-0B10000000000000000000000000000000)

    -0b10000000000000000000000000000001 checkType { check<Long>() }
    checkSubtype<Long>(-0b10000000000000000000000000000001)
}

// TESTCASE NUMBER: 5
fun case_5() {
    0b111111111111111111111111111111111111111111111111111111111111111 checkType { check<Long>() }
    checkSubtype<Long>(0b111111111111111111111111111111111111111111111111111111111111111)

    -0B111111111111111111111111111111111111111111111111111111111111111 checkType { check<Long>() }
    checkSubtype<Long>(-0B111111111111111111111111111111111111111111111111111111111111111)
}
