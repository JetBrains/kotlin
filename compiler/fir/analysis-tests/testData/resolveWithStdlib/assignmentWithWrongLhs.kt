// RUN_PIPELINE_TILL: FRONTEND

fun foo(): Int = 1

fun test() {
    <!VARIABLE_EXPECTED!>foo()<!> = 1
    foo().<!VARIABLE_EXPECTED!>toString()<!> = <!ASSIGNMENT_TYPE_MISMATCH!>1<!>
    <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(<!VARIABLE_EXPECTED!>if (true) foo() else 1<!>)<!> = <!ASSIGNMENT_TYPE_MISMATCH!>""<!>
    <!VARIABLE_EXPECTED!>run {
        print("To string").also { print(it) }
    }<!> = <!ASSIGNMENT_TYPE_MISMATCH!>1<!>
}
