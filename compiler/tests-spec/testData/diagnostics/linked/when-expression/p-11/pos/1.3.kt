
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Check when exhaustive via else entry (when with bound value, redundant else).
 * HELPERS: enumClasses, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: EnumClass): String = when (value_1) {
    EnumClass.EAST -> ""
    EnumClass.NORTH -> ""
    EnumClass.SOUTH -> ""
    EnumClass.WEST -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: EnumClass?): String = when (value_1) {
    EnumClass.EAST -> ""
    EnumClass.NORTH -> ""
    EnumClass.SOUTH -> ""
    EnumClass.WEST -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean): String = when (value_1) {
    true -> ""
    false -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Boolean?): String = when (value_1) {
    true -> ""
    false -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: SealedClass): String = when (value_1) {
    is SealedChild1 -> ""
    is SealedChild2 -> ""
    is SealedChild3 -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: SealedClass?): String = when (value_1) {
    is SealedChild1 -> ""
    is SealedChild2 -> ""
    is SealedChild3 -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// TESTCASE NUMBER: 7
fun case_7(value_1: SealedClassSingle): String = when (value_1) {
    <!USELESS_IS_CHECK!>is SealedClassSingle<!> -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// TESTCASE NUMBER: 8
fun case_8(value_1: SealedClassSingle?): String = when (value_1) {
    is SealedClassSingle -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}