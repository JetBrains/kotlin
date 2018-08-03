// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 19
 DESCRIPTION: 'When' with object literal in the control structure body.
 */

fun case_1(value: Int) {
    val __object_1 = object {
        val prop_1 = 1
    }

    when (value) {
        1 -> object {}
        2 -> object {
            var __lambda_1 = {
                when {
                    else -> true
                }
            }
            val __prop_1 = 1
        }
        3 -> __object_1
        4 -> {
            object {
                var __lambda_1 = {
                    when {
                        else -> true
                    }
                }
                val __prop_1 = __object_1
            }
        }
    }
}