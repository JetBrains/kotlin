// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: When with non-boolean value in the when condition.
 * HELPERS: typesProvider
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: String, value_3: TypesProvider): String {
    when {
        <!TYPE_MISMATCH!>.012f / value_1<!> -> return ""
        <!TYPE_MISMATCH!>"$value_2..."<!> -> return ""
        <!CONSTANT_EXPECTED_TYPE_MISMATCH!>'-'<!> -> return ""
        <!TYPE_MISMATCH!>{}<!> -> return ""
        <!TYPE_MISMATCH!>value_3.getAny()<!> -> return ""
        <!TYPE_MISMATCH!>-10..-1<!> -> return ""
    }

    return ""
}
