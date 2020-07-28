// SKIP_TXT


// TESTCASE NUMBER: 1
fun case_1(value_1: EnumClass?): String = when (value_1) {
    EnumClass.EAST -> ""
    EnumClass.NORTH -> ""
    EnumClass.SOUTH -> ""
    EnumClass.WEST -> ""
    null -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: EnumClassSingle?): String = when (value_1) {
    EnumClassSingle.EVERYTHING -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-26044
 */
fun case_3(value_1: EnumClassEmpty?): String = when(value_1) {
    null -> ""
}
