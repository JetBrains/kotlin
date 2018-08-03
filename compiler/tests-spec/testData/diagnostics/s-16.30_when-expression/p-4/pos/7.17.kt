/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 7: Any other expression.
 NUMBER: 17
 DESCRIPTION: 'When' with bound value and fun literals in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Any?): String {
    val __fun_1 = fun(): Int {
        return 0
    }

    return when (value) {
        fun() {} -> ""
        fun(): () -> () -> Unit {return fun(): () -> Unit {return fun() {}}} -> ""
        fun(): Boolean {
            return when {
                else -> true
            }
        } -> ""
        __fun_1 -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Any?): String {
    val __fun_1 = fun(): Int {
        return 0
    }

    when (value) {
        fun() {} -> return ""
        fun(): () -> () -> Unit {return fun(): () -> Unit {return fun() {}}} -> return ""
        fun(): Boolean {
            return when {
                else -> true
            }
        } -> return ""
        __fun_1 -> return ""
    }

    return ""
}