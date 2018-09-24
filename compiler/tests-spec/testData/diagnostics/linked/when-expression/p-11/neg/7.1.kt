// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_ENUM_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 11
 SENTENCE: [7] The bound expression is of an Enum classes type and all enumerated values are checked for equality using constant conditions;
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive when when not covered by all enumerated values.
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum (not all values).
fun case_1(value: _EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _EnumClass.EAST -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.NORTH -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum (not all values with enumerations).
fun case_2(value: _EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _EnumClass.EAST, _EnumClass.SOUTH, _EnumClass.NORTH -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum (one branch).
fun case_3(value: _EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _EnumClass.EAST -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum with several values (no branches).
fun case_4(value: _EnumClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value) { }

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum with one value (no branches).
fun case_5(value: _EnumClassSingle): String = <!NO_ELSE_IN_WHEN!>when<!>(value) { }

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' with all Enum values covered, but using variable.
 DISCUSSION: maybe use const propagation here?
 ISSUES: KT-25265
 */
fun case_6(value: _EnumClass): String {
    val west = _EnumClass.WEST

    return <!NO_ELSE_IN_WHEN!>when<!> (value) {
        _EnumClass.EAST -> ""
        _EnumClass.SOUTH -> ""
        _EnumClass.NORTH -> ""
        west -> ""
    }
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the empty enum class.
fun case_7(value: _EnumClassEmpty): String = <!NO_ELSE_IN_WHEN!>when<!> (value) { }
