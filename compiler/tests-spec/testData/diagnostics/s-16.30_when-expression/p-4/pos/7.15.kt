// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 7: Any other expression.
 NUMBER: 15
 DESCRIPTION: 'When' with bound value and call expressions in 'when condition'.
 */

class A {
    fun mul(value: Int): Int {
        return value * 2
    }
    fun nestedMul(value1: Int): (Int) -> Int {
        return fun(value2: Int): Int {
            return value1 * value2 * 2
        }
    }
}

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Int?, value1: List<Int>, value2: A, value3: A?): String {
    fun fun_1(): Int {
        return value1[0] + value1[1]
    }

    return when (value) {
        fun_1() -> ""
        value2.mul(value!!) -> ""
        value2.nestedMul(value)(value) -> ""
        value3?.nestedMul(value)?.invoke(value) -> ""
        value3!!.nestedMul(value)(value) -> ""
        value3?.mul(value) -> ""
        else -> ""
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Int?, value1: List<Int>, value2: A, value3: A?): String {
    fun fun_1(): Int {
        return value1[0] + value1[1]
    }

    when (value) {
        fun_1() -> return ""
        value2.mul(value!!) -> return ""
        value2.nestedMul(value)(value) -> return ""
        value3?.nestedMul(value)?.invoke(value) -> return ""
        value3?.mul(value) -> return ""
    }

    return ""
}