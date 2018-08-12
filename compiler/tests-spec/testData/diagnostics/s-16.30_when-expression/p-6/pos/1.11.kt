/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 11
 DESCRIPTION: 'When' with cast expression in the control structure body.
 */

fun case_1(value: Int, value1: Collection<Int>, value2: Collection<Int>?) {
    when (value) {
        1 -> value1 as MutableList<Int>
        2 -> value2 <!UNCHECKED_CAST!>as? MutableMap<Int, Int><!>
        3 -> (value1 <!UNCHECKED_CAST!>as? Map<Int, Int><!>) as MutableMap<Int, Int>
        4 -> {
            (value1 as List<Int>) as MutableList<Int>
        }
    }
}