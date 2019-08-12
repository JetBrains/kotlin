// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 8
 * NUMBER: 1
 * DESCRIPTION: Exhaustive when using enum values.
 * HELPERS: enumClasses
 */

// TESTCASE NUMBER: 1
fun case_1(dir: EnumClass): String = when (dir) {
    EnumClass.EAST -> ""
    EnumClass.NORTH -> ""
    EnumClass.SOUTH -> ""
    EnumClass.WEST -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: EnumClassSingle): String = when (value_1) {
    EnumClassSingle.EVERYTHING -> ""
}
