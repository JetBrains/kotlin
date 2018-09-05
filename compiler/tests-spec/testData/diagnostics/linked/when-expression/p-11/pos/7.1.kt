// !WITH_ENUM_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 11
 SENTENCE: [7] The bound expression is of an Enum classes type and all enumerated values are checked for equality using constant conditions;
 NUMBER: 1
 DESCRIPTION: Check when exhaustive when all enumerated values are checked.
 */

// CASE DESCRIPTION: Checking for exhaustive 'when' (all enum values covered).
fun case_1(dir: _EnumClass): String = when (dir) {
    _EnumClass.EAST -> ""
    _EnumClass.NORTH -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.WEST -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (single enum value covered).
fun case_2(value: _EnumClassSingle): String = when (value) {
    _EnumClassSingle.EVERYTHING -> ""
}
