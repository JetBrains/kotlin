package b

fun foo() {
    for (i in <!UNRESOLVED_REFERENCE!>collection<!>) {
        <!UNUSED_FUNCTION_LITERAL!>{
         <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
    }<!>
}<!SYNTAX!><!>