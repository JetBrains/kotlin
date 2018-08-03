// !WITH_CLASSES
// !WITH_FUNS

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 15
 DESCRIPTION: 'When' with call expression in the control structure body.
 */

fun case_1(value: Int, value1: _Class, value2: _Class?, value3: List<Int>, value4: List<Int>?) {
    fun __fun_1(): () -> Unit {
        return fun() {
            value4!![0] + value3[1]
        }
    }

    when (value) {
        1 -> _fun(value3, value4!!)
        2 -> __fun_1()()
        3 -> value1.fun_2(value3[0])
        4 -> value2?.fun_2(value3[0])
        5 -> {
            value2!!.fun_1()(value4!![0])
        }
    }
}