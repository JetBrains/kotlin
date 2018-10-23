// !CHECK_TYPE
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] An integer literal with the long literal mark has type kotlin.Long; an integer literal without it has one of the types kotlin.Int/kotlin.Short/kotlin.Byte (the selected type is dependent on the context), if its value is in range of the corresponding type, or type kotlin.Long otherwise.
 NUMBER: 1
 DESCRIPTION: Various integer literals with not allowed long literal mark in lower case (type checking).
 */

fun case_1() {
    0<!WRONG_LONG_SUFFIX!>l<!> checkType { _<Long>() }
    10000000000000<!WRONG_LONG_SUFFIX!>l<!> checkType { _<Long>() }
    0X000Af10cD<!WRONG_LONG_SUFFIX!>l<!> checkType { _<Long>() }
    0x0_0<!WRONG_LONG_SUFFIX!>l<!> checkType { _<Long>() }
    0b100_000_111_111<!WRONG_LONG_SUFFIX!>l<!> checkType { _<Long>() }
    0b0<!WRONG_LONG_SUFFIX!>l<!> checkType { _<Long>() }
}
