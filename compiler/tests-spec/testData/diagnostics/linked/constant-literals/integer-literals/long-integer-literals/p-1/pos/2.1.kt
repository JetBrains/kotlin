// !CHECK_TYPE
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] An integer literal with the long literal mark has type kotlin.Long; an integer literal without it has one of the types kotlin.Int/kotlin.Short/kotlin.Byte (the selected type is dependent on the context), if its value is in range of the corresponding type, or type kotlin.Long otherwise.
 NUMBER: 1
 DESCRIPTION: Type checking of various integer literals with long literal mark.
 */

fun case_1() {
    0L checkType { _<Long>() }
    10000000000000L checkType { _<Long>() }
    0X000Af10cDL checkType { _<Long>() }
    0x0_0L checkType { _<Long>() }
    0b100_000_111_111L checkType { _<Long>() }
    0b0L checkType { _<Long>() }
}

fun case_2() {
    9223372036854775807L checkType { _<Long>() }
    -9223372036854775807L checkType { _<Long>() }

    0X7FFFFFFFFFFFFFFFL checkType { _<Long>() }
    -0x7FFFFFFFFFFFFFFFL checkType { _<Long>() }

    0b111111111111111111111111111111111111111111111111111111111111111L checkType { _<Long>() }
    -0B111111111111111111111111111111111111111111111111111111111111111L checkType { _<Long>() }
}
