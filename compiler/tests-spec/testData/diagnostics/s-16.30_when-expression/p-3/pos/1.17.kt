// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 17
 DESCRIPTION: 'When' with fun literal in the control structure body.
 */

fun case_1(value: Int) {
    val fun_1 = fun(): Int {
        return 0
    }

    when {
        value == 1 -> fun() {}
        value == 2 -> fun(): Boolean {
            return when {
                else -> true
            }
        }
        value == 3 -> fun_1
        value == 4 -> {
            fun() {fun() {fun() {}}}
        }
    }
}