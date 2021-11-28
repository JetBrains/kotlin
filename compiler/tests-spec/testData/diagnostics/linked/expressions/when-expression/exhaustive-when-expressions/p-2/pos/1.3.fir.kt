// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: EnumClass): String = when (value_1) {
    EnumClass.EAST -> ""
    EnumClass.NORTH -> ""
    EnumClass.SOUTH -> ""
    EnumClass.WEST -> ""
    else -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: EnumClass?): String = when (value_1) {
    EnumClass.EAST -> ""
    EnumClass.NORTH -> ""
    EnumClass.SOUTH -> ""
    EnumClass.WEST -> ""
    null -> ""
    else -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean): String = when (value_1) {
    true -> ""
    false -> ""
    else -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Boolean?): String = when (value_1) {
    true -> ""
    false -> ""
    null -> ""
    else -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: SealedClass): String = when (value_1) {
    is SealedChild1 -> ""
    is SealedChild2 -> ""
    is SealedChild3 -> ""
    else -> ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: SealedClass?): String = when (value_1) {
    is SealedChild1 -> ""
    is SealedChild2 -> ""
    is SealedChild3 -> ""
    null -> ""
    else -> ""
}

// TESTCASE NUMBER: 7
fun case_7(value_1: SealedClassSingle): String = when (value_1) {
    <!USELESS_IS_CHECK!>is SealedClassSingle<!> -> ""
    else -> ""
}

// TESTCASE NUMBER: 8
fun case_8(value_1: SealedClassSingle?): String = when (value_1) {
    is SealedClassSingle -> ""
    null -> ""
    else -> ""
}
