/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 3
 SENTENCE: [2] Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 2
 DESCRIPTION: 'When' without bound value and only one 'else' branch.
 */

// CASE DESCRIPTION: 'When' as expression with only one 'else' branch.
fun case_1() = when {
    else -> ""
}

// CASE DESCRIPTION: 'When' as statement with only one 'else' branch.
fun case_2(): String {
    when {
        else -> return ""
    }
}