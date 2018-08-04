// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// !WITH_CLASSES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 15
 DESCRIPTION: 'When' with bound value and call expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Int?, value1: List<Int>, value2: _Class, value3: _Class?): String {
    fun fun_1(): Int {
        return value1[0] + value1[1]
    }

    return when (value) {
        fun_1() -> ""
        value2.fun_2(value!!) -> ""
        value2.fun_3(value)(value) -> ""
        value3?.fun_3(value)?.invoke(value) -> ""
        value3!!.fun_3(value)(value) -> ""
        value3?.fun_2(value) -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Int?, value1: List<Int>, value2: _Class, value3: _Class?): String {
    fun fun_1(): Int {
        return value1[0] + value1[1]
    }

    when (value) {
        fun_1() -> return ""
        value2.fun_2(value!!) -> return ""
        value2.fun_3(value)(value) -> return ""
        value3?.fun_3(value)?.invoke(value) -> return ""
        value3?.fun_2(value) -> return ""
    }

    return ""
}