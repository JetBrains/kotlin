
/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 13
 DESCRIPTION: 'When' with postfix operator expression in the control structure body.
 */

fun case_1(value: Int, value1: Int, value2: Int, value3: Boolean?, value4: Int?) {
    var mutableValue1 = value1
    var mutableValue2 = value2

    when {
        value == 1 -> <!UNUSED_CHANGED_VALUE!>mutableValue1++<!>
        value == 2 -> <!UNUSED_CHANGED_VALUE!>mutableValue2--<!>
        value == 3 -> <!UNUSED_CHANGED_VALUE!>mutableValue1--<!> - <!UNUSED_CHANGED_VALUE!>mutableValue2++<!>
        value == 5 -> mutableValue1++ + <!UNUSED_CHANGED_VALUE!>mutableValue1--<!>
        value == 5 -> !value3!!
        value == 7 -> {
            value4!! - <!UNUSED_CHANGED_VALUE!>mutableValue1--<!> - <!UNUSED_CHANGED_VALUE!>mutableValue2++<!>
        }
    }
}