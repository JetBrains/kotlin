// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 7
 * NUMBER: 1
 * DESCRIPTION: Checking for not exhaustive when when not covered by all enumerated values.
 * HELPERS: enumClasses
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    EnumClass.EAST -> ""
    EnumClass.SOUTH -> ""
    EnumClass.NORTH -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    EnumClass.EAST, EnumClass.SOUTH, EnumClass.NORTH -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    EnumClass.EAST -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) { }

// TESTCASE NUMBER: 5
fun case_5(value_1: EnumClassSingle): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) { }

/*
 * TESTCASE NUMBER: 6
 * DISCUSSION: maybe use const propagation here?
 * ISSUES: KT-25265
 */
fun case_6(value_1: EnumClass): String {
    val west = EnumClass.WEST

    return <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
        EnumClass.EAST -> ""
        EnumClass.SOUTH -> ""
        EnumClass.NORTH -> ""
        west -> ""
    }
}

/*
 * TESTCASE NUMBER: 7
 * DISCUSSION
 * ISSUES: KT-26044
 */
fun case_7(value_1: EnumClassEmpty): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) { }
