/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 2: It has an else entry;
 NUMBER: 3
 DESCRIPTION: Check when exhaustive via else entry (when with bound value, redundant else).
 */

enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

enum class Anything {
    EVERYTHING
}

sealed class Expr
data class Const(val number: Int) : Expr()
data class Sum(val e1: Int, val e2: Int) : Expr()
data class Mul(val m1: Int, val m2: Int) : Expr()

sealed class Expr2
data class Const2(val number: Int) : Expr2()

// CASE DESCRIPTION: Checking for redundant 'else' branch (all enum values covered).
fun case_1(value: Direction): String = when (value) {
    Direction.EAST -> ""
    Direction.NORTH -> ""
    Direction.SOUTH -> ""
    Direction.WEST -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (all enum values and null value covered).
fun case_2(value: Direction?): String = when (value) {
    Direction.EAST -> ""
    Direction.NORTH -> ""
    Direction.SOUTH -> ""
    Direction.WEST -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (single enum value covered).
fun case_3(value: Anything): String = when (value) {
    Anything.EVERYTHING -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (single enum value and null value covered).
fun case_4(value: Anything?): String = when (value) {
    Anything.EVERYTHING -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (both boolean value covered).
fun case_5(value: Boolean): String = when (value) {
    true -> ""
    false -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (both boolean value and null value covered).
fun case_6(value: Boolean?): String = when (value) {
    true -> ""
    false -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (all sealed class subtypes covered).
fun case_7(value: Expr): String = when (value) {
    is Const -> ""
    is Sum -> ""
    is Mul -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (all sealed class subtypes and null value covered).
fun case_8(value: Expr?): String = when (value) {
    is Const -> ""
    is Sum -> ""
    is Mul -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (single sealed class subtype covered).
fun case_9(value: Expr2): String = when (value) {
    is Const2 -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (single sealed class subtype and null value covered).
fun case_10(value: Expr2?): String = when (value) {
    is Const2 -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (sealed class itself covered).
fun case_11(value: Expr2): String = when (value) {
    <!USELESS_IS_CHECK!>is Expr2<!> -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (sealed class itself and null value covered).
fun case_12(value: Expr2?): String = when (value) {
    is Expr2 -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}