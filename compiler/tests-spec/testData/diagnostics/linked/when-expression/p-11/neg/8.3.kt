// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_ENUM_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 11
 * SENTENCE: [8] The bound expression is of a nullable type and one of the areas above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 * NUMBER: 3
 * DESCRIPTION: Checking for not exhaustive 'when' on the nullable enums.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: _EnumClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    _EnumClass.EAST -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.NORTH -> ""
    _EnumClass.WEST -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: _EnumClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    _EnumClass.EAST -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.NORTH -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: _EnumClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    _EnumClass.EAST, null, _EnumClass.SOUTH, _EnumClass.NORTH -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: _EnumClassSingle): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {}

// TESTCASE NUMBER: 5
fun case_5(value_1: _EnumClassSingle?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    _EnumClassSingle.EVERYTHING -> ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: _EnumClassSingle?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    null -> ""
}
