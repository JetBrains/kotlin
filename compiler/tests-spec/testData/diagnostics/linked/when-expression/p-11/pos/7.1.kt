
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 7
 * NUMBER: 1
 * DESCRIPTION: Check when exhaustive when all enumerated values are checked.
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
