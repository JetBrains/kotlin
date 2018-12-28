// !WITH_ENUM_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 8
 * NUMBER: 2
 * DESCRIPTION: Check when exhaustive when enumerated values are checked and contains a null check.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: _EnumClass?): String = when (value_1) {
    _EnumClass.EAST -> ""
    _EnumClass.NORTH -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.WEST -> ""
    null -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: _EnumClassSingle?): String = when (value_1) {
    _EnumClassSingle.EVERYTHING -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-26044
 */
fun case_3(value_1: _EnumClassEmpty?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    null -> ""
}
