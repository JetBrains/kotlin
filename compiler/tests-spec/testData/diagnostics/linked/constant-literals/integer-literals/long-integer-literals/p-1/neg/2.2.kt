// !CHECK_TYPE
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] An integer literal with the long literal mark has type kotlin.Long; an integer literal without it has one of the types kotlin.Int/kotlin.Short/kotlin.Byte (the selected type is dependent on the context), if its value is in range of the corresponding type, or type kotlin.Long otherwise.
 NUMBER: 2
 DESCRIPTION: Type checking (comparison with invalid types) of various integer literals with long literal mark.
 */

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
