
/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 11
 DESCRIPTION: 'When' with cast expression in the control structure body.
 */

fun case_1(value: Int, value1: Collection<Int>, value2: Collection<Int>?) {
    when {
        value == 1 -> value1 as MutableList<Int>
        value == 2 -> value2 <!UNCHECKED_CAST!>as? MutableMap<Int, Int><!>
        value == 3 -> (value1 <!UNCHECKED_CAST!>as? Map<Int, Int><!>) as MutableMap<Int, Int>
        value == 4 -> {
            (value1 as List<Int>) as MutableList<Int>
        }
    }
}