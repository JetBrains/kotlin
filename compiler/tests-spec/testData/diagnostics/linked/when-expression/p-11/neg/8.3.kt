// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 8
 * NUMBER: 3
 * DESCRIPTION: Checking for not exhaustive 'when' on the nullable enums.
 * HELPERS: enumClasses
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: EnumClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    EnumClass.EAST -> ""
    EnumClass.SOUTH -> ""
    EnumClass.NORTH -> ""
    EnumClass.WEST -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: EnumClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    EnumClass.EAST -> ""
    EnumClass.SOUTH -> ""
    EnumClass.NORTH -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: EnumClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    EnumClass.EAST, null, EnumClass.SOUTH, EnumClass.NORTH -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: EnumClassSingle): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {}

// TESTCASE NUMBER: 5
fun case_5(value_1: EnumClassSingle?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    EnumClassSingle.EVERYTHING -> ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: EnumClassSingle?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    null -> ""
}
