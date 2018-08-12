/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 8
 DESCRIPTION: 'When' with bound value and try expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Int?): String = when (value) {
    try {
        4
    } catch (e: Exception) {
        5
    } -> ""
    try {
        throw Exception()
    } catch (e: Exception) {
        6
    } finally {
        <!UNUSED_EXPRESSION!>7<!>
    } -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Int?): String {
    when (value) {
        try {
            4
        } catch (e: Exception) {
            5
        } -> return ""
        try {
            throw Exception()
        } catch (e: Exception) {
            6
        } finally {
            <!UNUSED_EXPRESSION!>7<!>
        } -> return ""
        else -> return ""
    }

    <!UNREACHABLE_CODE!>return ""<!>
}