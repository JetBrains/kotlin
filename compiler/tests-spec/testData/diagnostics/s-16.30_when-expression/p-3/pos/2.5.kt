/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 5
 DESCRIPTION: 'When' without bound value and only one 'else' branch.
 */

// CASE DESCRIPTION: 'When' with only one 'else' branch ('when' used as expression).
fun case_1(): String = when {
    else -> ""
}

// CASE DESCRIPTION: 'When' with only one 'else' branch ('when' used as statement).
fun case_2(): String {
    when {
        else -> {return ""}
    }
}