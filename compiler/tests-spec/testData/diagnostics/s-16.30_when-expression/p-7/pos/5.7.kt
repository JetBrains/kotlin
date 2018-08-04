/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 7
 DESCRIPTION: 'When' with bound value and if expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Int?, value1: Int, value2: Boolean): String = when (value) {
    if (value1 > 10) {
        1
    } else if (value1 < -10) {
        2
    } else {
        3
    } -> ""
    null -> ""
    if (value2) 10 else 100 -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Int?, value1: Int, value2: Boolean): String {
    when (value) {
        if (value1 > 10) {
            1
        } else if (value1 < -10) {
            2
        } else {
            3
        } -> return ""
        null -> return ""
        if (value2) 10 else 100 -> return ""
    }

    return ""
}