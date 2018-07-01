// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 9
 DESCRIPTION: 'When' with elvis operator expression in the control structure body.
 */

fun case_1(value: Int, value1: String?, value2: String?, value3: String?) {
    when {
        value == 1 -> value1 ?: true
        value == 2 -> value1 ?: value2 ?: true
        value == 3 -> value1 ?: value2 ?: value3 ?: true
        value == 4 -> value1!! <!USELESS_ELVIS!>?: true<!>
        value == 5 -> {
            value2 ?: value3 ?: true
        }
    }
}