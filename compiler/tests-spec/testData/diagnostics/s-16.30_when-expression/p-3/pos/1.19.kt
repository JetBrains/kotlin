// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 19
 DESCRIPTION: 'When' with object literal in the control structure body.
 */

fun case_1(value: Int) {
    val object1 = object {
        val prop1 = 1
    }

    when {
        value == 1 -> object {}
        value == 2 -> object {
            var lambda1 = {
                when {
                    else -> true
                }
            }
            val prop1 = 1
        }
        value == 3 -> object1
        value == 4 -> {
            object {
                var lambda1 = {
                    when {
                        else -> true
                    }
                }
                val prop1 = object1
            }
        }
    }
}