// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 9: The bound expression is of a nullable type and one of the cases above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 2
 DESCRIPTION: Check when exhaustive when enumerated values are checked and contains a null check.
 */

enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

enum class Anything {
    EVERYTHING
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (both enum values and null value covered).
fun case_1(dir: Direction?): String = when (dir) {
    Direction.EAST -> ""
    Direction.NORTH -> ""
    Direction.SOUTH -> ""
    Direction.WEST -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (single enum value and null value covered).
fun case_2(value: Anything?): String = when (value) {
    Anything.EVERYTHING -> ""
    null -> ""
}
