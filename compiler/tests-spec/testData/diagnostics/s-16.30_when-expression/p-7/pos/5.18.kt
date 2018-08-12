/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 18
 DESCRIPTION: 'When' with bound value and lambda literals in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Any?): String {
    val __lambda_1 = { 0 }

    return when (value) {
        {} -> ""
        {{{{{ 0 }}}}} -> ""
        {
            when {
                else -> true
            }
        } -> ""
        __lambda_1 -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Any?): String {
    val __lambda_1 = { 0 }

    return when (value) {
        {} -> ""
        {{{{{ 0 }}}}} -> ""
        {
            when {
                else -> true
            }
        } -> ""
        __lambda_1 -> ""
        else -> ""
    }
}