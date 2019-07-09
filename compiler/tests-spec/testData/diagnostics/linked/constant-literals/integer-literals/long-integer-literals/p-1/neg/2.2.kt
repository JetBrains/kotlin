// !CHECK_TYPE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Type checking (comparison with invalid types) of various integer literals with long literal mark.
 */

// TESTCASE NUMBER: 1
fun case_1() {
    0L checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    1000000L checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    0XAf10cDL checkType { <!TYPE_MISMATCH!>_<!><Int>() }
    0x0_0L checkType { <!TYPE_MISMATCH!>_<!><Byte>() }
    0b100_000_111_111L checkType { <!TYPE_MISMATCH!>_<!><Short>() }
    0b0L checkType { <!TYPE_MISMATCH!>_<!><Byte>() }

    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0L<!>)
    checkSubtype<Int>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1000000L<!>)
    checkSubtype<Int>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0XAf10cDL<!>)
    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0x0_0L<!>)
    checkSubtype<Short>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0b100_000_111_111L<!>)
    checkSubtype<Byte>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>0b0L<!>)
}
