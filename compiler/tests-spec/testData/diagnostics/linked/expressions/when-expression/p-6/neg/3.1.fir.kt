// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: TypesProvider): String {
    when (value_1) {
        <!INCOMPATIBLE_TYPES!>-1000L..100<!> -> return ""
        <!INCOMPATIBLE_TYPES!>value_2.getInt()..getLong()<!> -> return ""
    }

    return ""
}
