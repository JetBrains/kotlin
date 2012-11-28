fun foo(x: Boolean) {
    x <!UNRESOLVED_REFERENCE!><<!> x
    x <!UNRESOLVED_REFERENCE!><=<!> x
    x <!UNRESOLVED_REFERENCE!>><!> x
    x <!UNRESOLVED_REFERENCE!>>=<!> x
    x == x
    x.<!UNRESOLVED_REFERENCE!>compareTo<!>(x)
}
