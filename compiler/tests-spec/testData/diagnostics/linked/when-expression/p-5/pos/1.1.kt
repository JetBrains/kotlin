/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 5
 SENTENCE: [1] The else entry is also special in the sense that it must be the last entry in the expression, otherwise a compiler error must be generated.
 NUMBER: 1
 DESCRIPTION: 'When' without bound value and with else branch in the last position.
 */

// CASE DESCRIPTION: 'When' with else branch as statement
fun case_1(value: Int): String {
    when {
        value == 1 -> return ""
        value == 2 -> return ""
        else -> return ""
    }
}

// CASE DESCRIPTION: 'When' with else branch as expression
fun case_2(value: Int): String = when {
    value == 1 -> ""
    value == 2 -> ""
    else -> ""
}
