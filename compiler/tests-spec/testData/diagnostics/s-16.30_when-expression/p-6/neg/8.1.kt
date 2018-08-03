// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !CHECK_TYPE
// !WITH_ENUM_CLASSES

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 8: The bound expression is of an Enum classes type and all enumerated values are checked for equality using constant conditions;
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive when when not covered by all enumerated values.
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum (several branches).
fun case_1(value: _EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _EnumClass.EAST -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.NORTH -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum (one branch).
fun case_2(value: _EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _EnumClass.EAST -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum with several values (no branches).
fun case_3(value: _EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value) { }

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum with one value (no branches).
fun case_4(value: _EnumClassSingle): String = <!NO_ELSE_IN_WHEN!>when<!>(value) { }
