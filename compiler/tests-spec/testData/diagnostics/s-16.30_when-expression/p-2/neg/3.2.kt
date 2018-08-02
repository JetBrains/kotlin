
/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 2
 SENTENCE 3: When expression has two different forms: with bound value and without it.
 NUMBER: 2
 DESCRIPTION: Empty 'when' with missed 'when entries' section.
 */

// CASE DESCRIPTION: 'When' with bound value.
fun case_1(value: Int) {
    when (<!UNUSED_EXPRESSION!>value<!>)<!SYNTAX!><!>
}

// CASE DESCRIPTION: 'When' without bound value, but with parentheses.
fun case_2() {
    when (<!SYNTAX!><!>)<!SYNTAX!><!>
}

// CASE DESCRIPTION: 'When' without bound value and parentheses.
fun case_3() {
    when<!SYNTAX!><!>
}
