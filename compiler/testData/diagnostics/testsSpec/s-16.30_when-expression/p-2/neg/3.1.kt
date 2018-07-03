// SKIP_TXT

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 2
 SENTENCE 3: When expression has two different forms: with bound value and without it.
 NUMBER: 1
 DESCRIPTION: Empty 'when' with missed bound value.
 */

fun case_1() {
    when (<!SYNTAX!><!>) {}
}
