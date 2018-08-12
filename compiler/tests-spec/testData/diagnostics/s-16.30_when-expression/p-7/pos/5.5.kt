/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 5
 DESCRIPTION: 'When' with bound value and concatenations in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: String): String = when (value) {
    "" -> ""
    " " + "1" -> ""
    " $value " + "2" -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: String): String {
    when (value) {
        "" -> return ""
        " " + "1" -> return ""
        " $value " + "2" -> return ""
    }

    return ""
}