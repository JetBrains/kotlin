// !CHECK_TYPE
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] An integer literal with the long literal mark has type kotlin.Long; an integer literal without it has one of the types kotlin.Int/kotlin.Short/kotlin.Byte (the selected type is dependent on the context), if its value is in range of the corresponding type, or type kotlin.Long otherwise.
 NUMBER: 4
 DESCRIPTION: Type checking (comparison with invalid types) of too a big integers.
 */

fun case_1() {
    checkSubtype<Long>(<!TYPE_MISMATCH!>-<!INT_LITERAL_OUT_OF_RANGE!>9223372036854775808L<!><!>)
    -<!INT_LITERAL_OUT_OF_RANGE!>9223372036854775808L<!> checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Long>(<!INT_LITERAL_OUT_OF_RANGE!>9223372036854775808L<!>)
    <!INT_LITERAL_OUT_OF_RANGE!>9223372036854775808L<!> checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Long>(<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000L<!>)
    <!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000L<!> checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Long>(<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000l<!>)
    <!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000l<!> checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Long>(<!TYPE_MISMATCH!>-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000L<!><!>)
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000L<!> checkType { <!TYPE_MISMATCH!>_<!><Long>() }

    checkSubtype<Long>(<!TYPE_MISMATCH!>-<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000l<!><!>)
    -<!INT_LITERAL_OUT_OF_RANGE!>100000000000000000000000000000000l<!> checkType { <!TYPE_MISMATCH!>_<!><Long>() }
}
