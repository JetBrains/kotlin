// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_ENUM_CLASSES

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 9: The bound expression is of a nullable type and one of the cases above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 3
 DESCRIPTION: Checking for not exhaustive when when covered by all enumerated values, but no null check (or with no null check, but not covered by all enumerated values).
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum class without null-check branch.
fun case_1(value: _EnumClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _EnumClass.EAST -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.NORTH -> ""
    _EnumClass.WEST -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum class with null-check branch, but all possible values not covered.
fun case_2(value: _EnumClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _EnumClass.EAST -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.NORTH -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum class without branches.
fun case_3(value: _EnumClassSingle): Int = <!NO_ELSE_IN_WHEN!>when<!>(value) {}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum class (with only one value) without null-check branch.
fun case_4(value: _EnumClassSingle?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _EnumClassSingle.EVERYTHING -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Enum class (with only one value) with null-check branch, but value not covered.
fun case_5(value: _EnumClassSingle?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    null -> ""
}
