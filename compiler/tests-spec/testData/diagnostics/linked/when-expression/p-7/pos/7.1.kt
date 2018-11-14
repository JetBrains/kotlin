/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 7
 SENTENCE: [7] The else condition, which works the exact same way as it would in the form without bound expression.
 NUMBER: 1
 DESCRIPTION: 'When' with bound value and else branch.
 */

// CASE DESCRIPTION: Simple when with bound value, with 'else' branch and expression as when condition.
fun case_1(value_1: Int?) = when (value_1) {
    0 -> ""
    1 -> ""
    2 -> ""
    else -> ""
}

// CASE DESCRIPTION: Simple when with bound value, with 'else' branch and type test as when condition.
fun case_2(value_1: Any) = when (value_1) {
    is Int -> ""
    is Boolean -> ""
    is String -> ""
    else -> ""
}

// CASE DESCRIPTION: Simple when with bound value, with 'else' branch and range test as when condition.
fun case_2(value_1: Int) = when (value_1) {
    in -10..10 -> ""
    in 11..1000 -> ""
    in 1000..Int.MAX_VALUE -> ""
    else -> ""
}