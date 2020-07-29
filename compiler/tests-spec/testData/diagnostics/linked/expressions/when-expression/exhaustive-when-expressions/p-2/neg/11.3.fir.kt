// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
fun case_1(value_1: EnumClass?): String = when(value_1) {
    EnumClass.EAST -> ""
    EnumClass.SOUTH -> ""
    EnumClass.NORTH -> ""
    EnumClass.WEST -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: EnumClass?): String = when(value_1) {
    EnumClass.EAST -> ""
    EnumClass.SOUTH -> ""
    EnumClass.NORTH -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: EnumClass?): String = when(value_1) {
    EnumClass.EAST, null, EnumClass.SOUTH, EnumClass.NORTH -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: EnumClassSingle): Int = when(value_1) {}

// TESTCASE NUMBER: 5
fun case_5(value_1: EnumClassSingle?): String = when(value_1) {
    EnumClassSingle.EVERYTHING -> ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: EnumClassSingle?): String = when(value_1) {
    null -> ""
}
