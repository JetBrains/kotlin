// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: String, value_3: TypesProvider): String {
    when {
        <!CONDITION_TYPE_MISMATCH, TYPE_MISMATCH!>.012f / value_1<!> -> return ""
        <!CONDITION_TYPE_MISMATCH!>"$value_2..."<!> -> return ""
        <!CONDITION_TYPE_MISMATCH!>'-'<!> -> return ""
        <!CONDITION_TYPE_MISMATCH!>{}<!> -> return ""
        <!CONDITION_TYPE_MISMATCH, TYPE_MISMATCH!>value_3.getAny()<!> -> return ""
        <!CONDITION_TYPE_MISMATCH, TYPE_MISMATCH!>-10..-1<!> -> return ""
    }

    return ""
}
