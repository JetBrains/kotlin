/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 2
 DESCRIPTION: 'When' without bound value and with invalid 'else' branch.
 */

// CASE DESCRIPTION: 'When' with only one invalid 'else' branch.
fun case_1() {
    when {
        else -><!SYNTAX!><!>
    }
}

// CASE DESCRIPTION: 'When' with only two invalid 'else' branches.
fun case_2() {
    when {
        else -><!SYNTAX!><!>
        else -><!SYNTAX!><!>
    }
}

// CASE DESCRIPTION: 'When' with two not 'else' valid branches and invalid 'else' branch.
fun case_3(value: Int) {
    when {
        value == 1 -> println("1")
        value == 2 -> println("2")
        else -><!SYNTAX!><!>
    }
}

// CASE DESCRIPTION: 'When' with one not 'else' valid branch and invalid 'else' branch.
fun case_4(value: Int) {
    when {
        value == 1 -> println("!")
        else -><!SYNTAX!><!>
    }
}