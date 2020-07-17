// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: String, value_3: TypesProvider): String {
    when {
        .012f / value_1 -> return ""
        "$value_2..." -> return ""
        '-' -> return ""
        {} -> return ""
        value_3.getAny() -> return ""
        -10..-1 -> return ""
    }

    return ""
}
