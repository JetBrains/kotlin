
/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 15
 DESCRIPTION: 'When' with call expression in the control structure body.
 */

class A {
    val prop_1 = 1
    val prop_2 = 2
    val prop_3 = 3

    fun mul(value: Int): Int {
        return value * 2
    }

    fun fun_2(): (Int) -> (Int) -> Int {
        return {number: Int -> { number * 5 }}
    }
}

fun fun_1(value1: List<Int>, value2: List<Int>): Int {
    return value1[0] + value2[1]
}

fun case_1(value: Int, value1: A, value2: A?, value3: List<Int>, value4: List<Int>?) {
    fun fun_2(): () -> Unit {
        return fun() {
            value4!![0] + value3[1]
        }
    }

    when (value) {
        1 -> fun_1(value3, value4!!)
        2 -> fun_2()()
        3 -> value1.mul(value3[0])
        4 -> value2?.mul(value3[0])
        5 -> {
            value2!!.fun_2()(value4!![0])
        }
    }
}