// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT



// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean?): String = when(value_1) {
    true -> ""
    false -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean?): String = when(value_1) {
    true -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean?): Int = when(value_1) { }
