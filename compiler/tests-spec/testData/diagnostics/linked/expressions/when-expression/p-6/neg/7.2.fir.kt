// SKIP_TXT


// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String = when {
    else -> ""
    value_1 == 1 -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String = when {
    value_1 == 1 -> ""
    else -> ""
    value_1 == 2 -> ""
}

// TESTCASE NUMBER: 3
fun case_3(): String {
    when {
        else -> return ""
        else -> return ""
    }

    return ""
}
