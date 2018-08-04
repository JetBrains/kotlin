// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 7: The else condition, which works the exact same way as it would in the form without bound expression.
 NUMBER: 1
 DESCRIPTION: 'When' with invalid else condition.
 */

// CASE DESCRIPTION: 'When' with only one invalid 'else' branch.
fun case_1(value: Int) {
    when (value) {
        else -><!SYNTAX!><!>
    }
}

// CASE DESCRIPTION: 'When' with only two invalid 'else' branches.
fun case_2(value: Int) {
    when (value) {
        else -><!SYNTAX!><!>
        else -><!SYNTAX!><!>
    }
}

// CASE DESCRIPTION: 'When' with two not 'else' valid branches and invalid 'else' branch.
fun case_3(value: Int) {
    when (value) {
        1 -> println("1")
        2 -> println("2")
        else -><!SYNTAX!><!>
    }
}

// CASE DESCRIPTION: 'When' with one not 'else' valid branch and invalid 'else' branch.
fun case_4(value: Int) {
    when (value) {
        1 -> println("!")
        else -><!SYNTAX!><!>
    }
}