// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 19
 DESCRIPTION: 'When' with bound value and object literals in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Any?): String {
    val __object_1 = object {
        val __prop_1 = 1
    }

    return when (value) {
        object {} -> ""
        object {
            val __object_2 = object {
                val __object_3 = object {}
            }
        } -> ""
        object {
            var __lambda_1 = {
                when {
                    else -> true
                }
            }
            val __prop_1 = 1
        } -> ""
        __object_1 -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Any?): String {
    val __object_1 = object {
        val __prop_1 = 1
    }

    when (value) {
        object {} -> return ""
        object {
            val __object_2 = object {
                val __object_3 = object {}
            }
        } -> return ""
        object {
            var __lambda_1 = {
                when {
                    else -> true
                }
            }
            val __prop_1 = 1
        } -> return ""
        __object_1 -> return ""
    }

    return ""
}
