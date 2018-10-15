// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 11
 SENTENCE: [1] It has an else entry;
 NUMBER: 2
 DESCRIPTION: Check when exhaustive via else entry (when with bound value).
 */

// CASE DESCRIPTION: Checking for exhaustive 'when' (several branches).
fun case_1(value_1: Int): String = when (value_1) {
    0 -> ""
    1 -> ""
    2 -> ""
    3 -> ""
    else -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (value check branch and 'else' branch).
fun case_2(value_1: Boolean): String = when (value_1) {
    true -> ""
    else -> ""
}

/*
 CASE DESCRIPTION: Checking for exhaustive 'when' with constant bound value (value check branch and 'else' branch).
 NOTE: for potential bound value constant analysis.
 */
fun case_3(): String = when (true) {
    true -> ""
    else -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (only 'else' branch).
fun case_4(value_1: Int): String = when(value_1) {
    else -> ""
}