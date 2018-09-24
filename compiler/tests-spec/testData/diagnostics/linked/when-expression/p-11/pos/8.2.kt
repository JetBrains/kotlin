// !WITH_ENUM_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 11
 SENTENCE: [8] The bound expression is of a nullable type and one of the cases above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 2
 DESCRIPTION: Check when exhaustive when enumerated values are checked and contains a null check.
 */

// CASE DESCRIPTION: Checking for exhaustive 'when' (both enum values and null value covered).
fun case_1(dir: _EnumClass?): String = when (dir) {
    _EnumClass.EAST -> ""
    _EnumClass.NORTH -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.WEST -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (single enum value and null value covered).
fun case_2(value: _EnumClassSingle?): String = when (value) {
    _EnumClassSingle.EVERYTHING -> ""
    null -> ""
}
