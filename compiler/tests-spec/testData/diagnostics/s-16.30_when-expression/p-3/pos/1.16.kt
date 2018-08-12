// !WITH_CLASSES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 16
 DESCRIPTION: 'When' with property access expression in the control structure body.
 */

fun case_1(value: Int, value1: _Class, value2: _Class?) {
    when {
        value == 1 -> value1.prop_1
        value == 2 -> value2?.prop_1
        value == 3 -> value1::prop_1.get()
        value == 4 -> {
            value2!!::prop_3.get()
        }
    }
}