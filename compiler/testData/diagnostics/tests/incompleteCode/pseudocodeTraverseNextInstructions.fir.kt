package b

fun foo() {
    for (i in <!ITERATOR_MISSING, UNRESOLVED_REFERENCE!>collection<!>) {
        {
         <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
    }
}<!SYNTAX!><!>
