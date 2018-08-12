/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 14
 DESCRIPTION: 'When' with bound value and indexing expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Int?, value1: List<Int>, value2: List<List<List<List<Int>>?>>, value3: List<List<() -> List<List<Int>>>>, value4: List<List<(() -> List<List<Int>>)?>>): String = when (value) {
    value1[0] -> ""
    value1[1] -> ""
    value2[0][11]!![-90][0L.toInt()] -> ""
    value3[0][11]()[-90][0L.toInt()] -> ""
    value4[0][11]!!()[-90][0L.toInt()] -> ""
    null -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Int?, value1: List<Int>, value2: List<List<List<List<Int>>?>>, value3: List<List<() -> List<List<Int>>>>, value4: List<List<(() -> List<List<Int>>)?>>): String {
    when (value) {
        value1[0] -> return ""
        value1[1] -> return ""
        value2[0][11]!![-90][0L.toInt()] -> return ""
        value3[0][11]()[-90][0L.toInt()] -> return ""
        value4[0][11]!!()[-90][0L.toInt()] -> return ""
        null -> return ""
    }

    return ""
}