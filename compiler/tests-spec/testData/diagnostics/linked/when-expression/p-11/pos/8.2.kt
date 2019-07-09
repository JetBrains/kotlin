
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 8
 * NUMBER: 2
 * DESCRIPTION: Check when exhaustive when enumerated values are checked and contains a null check.
 * HELPERS: enumClasses
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: EnumClass?): String = when (value_1) {
    EnumClass.EAST -> ""
    EnumClass.NORTH -> ""
    EnumClass.SOUTH -> ""
    EnumClass.WEST -> ""
    null -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: EnumClassSingle?): String = when (value_1) {
    EnumClassSingle.EVERYTHING -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-26044
 */
fun case_3(value_1: EnumClassEmpty?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    null -> ""
}
