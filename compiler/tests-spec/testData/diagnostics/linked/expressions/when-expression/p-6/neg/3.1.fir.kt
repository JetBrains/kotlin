// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: TypesProvider): String {
    when (value_1) {
        -1000L..100 -> return ""
        value_2.getInt()..getLong() -> return ""
    }

    return ""
}
