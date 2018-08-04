/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 6
 DESCRIPTION: 'When' with bound value and when expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: String?, value1: Int, value2: Boolean): String = when (value) {
    when {
        value1 > 10 -> "1"
        else -> "2"
    } -> ""
    null -> ""
    when (value2) {
        true -> "10"
        false -> "100"
    } -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: String?, value1: Int, value2: Boolean): String {
    when (value) {
        when {
            value1 > 10 -> "1"
            else -> "2"
        } -> return ""
        null -> return ""
        when (value2) {
            true -> "10"
            false -> "100"
        } -> return ""
    }

    return ""
}