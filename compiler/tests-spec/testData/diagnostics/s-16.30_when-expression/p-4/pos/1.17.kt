// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 17
 DESCRIPTION: 'When' with fun literal in the control structure body.
 */

fun case_1(value: Int) {
    val __fun_1 = fun(): Int {
        return 0
    }

    when (value) {
        1 -> fun() {}
        2 -> fun(): Boolean {
            return when {
                else -> true
            }
        }
        3 -> __fun_1
        4 -> {
            fun() {fun() {fun() {}}}
        }
    }
}