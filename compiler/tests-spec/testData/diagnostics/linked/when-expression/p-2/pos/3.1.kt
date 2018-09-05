/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 2
 SENTENCE: [3] When expression has two different forms: with bound value and without it.
 NUMBER: 1
 DESCRIPTION: Empty 'when' with bound value.
 */

fun case_1(value: Int) {
    when (<!UNUSED_EXPRESSION!>value<!>) {}
}
