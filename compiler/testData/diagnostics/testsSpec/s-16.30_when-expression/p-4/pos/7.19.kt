// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 7: Any other expression.
 NUMBER: 19
 DESCRIPTION: 'When' with bound value and object literals in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Any?): String {
    val object1 = object {
        val prop1 = 1
    }

    return when (value) {
        object {} -> ""
        object {
            val o1 = object {
                val o2 = object {}
            }
        } -> ""
        object {
            var lambda1 = {
                when {
                    else -> true
                }
            }
            val prop1 = 1
        } -> ""
        object1 -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Any?): String {
    val object1 = object {
        val prop1 = 1
    }

    when (value) {
        object {} -> return ""
        object {
            val o1 = object {
                val o2 = object {}
            }
        } -> return ""
        object {
            var lambda1 = {
                when {
                    else -> true
                }
            }
            val prop1 = 1
        } -> return ""
        object1 -> return ""
    }

    return ""
}
