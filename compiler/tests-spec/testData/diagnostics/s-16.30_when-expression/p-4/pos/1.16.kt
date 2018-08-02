/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 16
 DESCRIPTION: 'When' with property access expression in the control structure body.
 */

class A {
    val prop_1 = 1
    val prop_2 = 2
    val prop_3 = 3
}

fun case_1(value: Int, value1: A, value2: A?) {
    when (value) {
        1 -> value1.prop_1
        2 -> value2?.prop_1
        3 -> value1::prop_1.get()
        4 -> {
            value2!!::prop_3.get()
        }
    }
}