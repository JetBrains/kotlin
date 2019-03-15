// !CHECK_TYPE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Type checking of various integer literals with long literal mark.
 */

// TESTCASE NUMBER: 1
fun case_1() {
    0L checkType { _<Long>() }
    10000000000000L checkType { _<Long>() }
    0X000Af10cDL checkType { _<Long>() }
    0x0_0L checkType { _<Long>() }
    0b100_000_111_111L checkType { _<Long>() }
    0b0L checkType { _<Long>() }
}

// TESTCASE NUMBER: 2
fun case_2() {
    9223372036854775807L checkType { _<Long>() }
    -9223372036854775807L checkType { _<Long>() }

    0X7FFFFFFFFFFFFFFFL checkType { _<Long>() }
    -0x7FFFFFFFFFFFFFFFL checkType { _<Long>() }

    0b111111111111111111111111111111111111111111111111111111111111111L checkType { _<Long>() }
    -0B111111111111111111111111111111111111111111111111111111111111111L checkType { _<Long>() }
}
