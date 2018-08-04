/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 8
 SENTENCE 10: This also means that if this form of when contains a boolean expression, it is not checked directly as if it would be in the other form, but rather checked for equality with the bound variable, which is not the same thing.
 NUMBER: 1
 DESCRIPTION: 'When' with boolean bound value and true/false checks.
 */

fun case_1(value: Boolean): String = when (value) {
    true -> ""
    false -> ""
}
