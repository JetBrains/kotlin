/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 11
 DESCRIPTION: 'When' with bound value and cast expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Collection<Int>?, value1: Collection<Int>, value2: Collection<Int>?): String = when (value) {
    value1 as MutableList<Int> -> ""
    value2 <!UNCHECKED_CAST!>as? MutableMap<Int, Int><!> -> ""
    (value2 <!UNCHECKED_CAST!>as? Map<Int, Int><!>) as MutableMap<Int, Int> -> ""
    null -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Collection<Int>?, value1: Collection<Int>, value2: Collection<Int>?): String {
    when (value) {
        value1 as MutableList<Int> -> return ""
        value2 <!UNCHECKED_CAST!>as? MutableMap<Int, Int><!> -> return ""
        (value2 <!UNCHECKED_CAST!>as? Map<Int, Int><!>) as MutableMap<Int, Int> -> return ""
        null -> return ""
    }

    return ""
}
