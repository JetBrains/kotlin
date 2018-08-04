// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 9
 DESCRIPTION: 'When' with elvis operator expression in the control structure body.
 */

fun case_1(value: Int, value1: String?, value2: String?, value3: String?) {
    when (value) {
        1 -> value1 ?: true
        2 -> value1 ?: value2 ?: true
        3 -> value1 ?: value2 ?: value3 ?: true
        4 -> value1!! <!USELESS_ELVIS!>?: true<!>
        5 -> {
            value2 ?: value3 ?: true
        }
    }
}