// !WITH_ENUM_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 11
 SENTENCE: [8] The bound expression is of a nullable type and one of the cases above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 2
 DESCRIPTION: Check when exhaustive when enumerated values are checked and contains a null check.
 */

// CASE DESCRIPTION: Checking for exhaustive 'when' (both enum values and null value covered).
fun case_1(value_1: _EnumClass?): String = when (value_1) {
    _EnumClass.EAST -> ""
    _EnumClass.NORTH -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.WEST -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (single enum value and null value covered).
fun case_2(value_1: _EnumClassSingle?): String = when (value_1) {
    _EnumClassSingle.EVERYTHING -> ""
    null -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' on the empty nullable enum class.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26044
 */
fun case_3(value_1: _EnumClassEmpty?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    null -> ""
}
