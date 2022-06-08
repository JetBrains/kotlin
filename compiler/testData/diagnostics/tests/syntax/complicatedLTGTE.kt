// SKIP_TXT
// ISSUE: KT-52684

fun test(x: Int, y: Int) {
    if (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!><!UNRESOLVED_REFERENCE!>x<!> < (<!SYNTAX!>if<!><!SYNTAX!><!> <!SYNTAX!>(<!><!UNRESOLVED_REFERENCE!><!SYNTAX!><!>y<!> >= 115<!>) 1 else 2<!SYNTAX!>))<!> {
        Unit
    }
}
