/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 16
 DESCRIPTION: 'When' with property access expression in the control structure body.
 */

class A {
    val prop1 = 1
    val prop2 = 2
    val prop3 = 3
}

fun case_1(value: Int, value1: A, value2: A?) {
    when {
        value == 1 -> value1.prop1
        value == 2 -> value2?.prop1
        value == 3 -> value1::prop1.get()
        value == 4 -> {
            value2!!::prop3.get()
        }
    }
}