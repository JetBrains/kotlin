// !WITH_CLASSES
// !WITH_FUNS

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 15
 DESCRIPTION: 'When' with call expression in the control structure body.
 */

fun case_1(value: Int, value1: _Class, value2: _Class?, value3: List<Int>, value4: List<Int>?) {
    fun __fun_2(): () -> Unit {
        return fun() {
            value4!![0] + value3[1]
        }
    }

    when {
        value == 1 -> _fun(value3, value4!!)
        value == 2 -> __fun_2()()
        value == 3 -> value1.fun_2(value3[0])
        value == 4 -> value2?.fun_2(value3[0])
        value == 5 -> {
            value2!!.fun_1()(value4!![0])
        }
    }
}