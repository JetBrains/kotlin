val a: Int
    get() = 10

fun test() {
    a.<!UNRESOLVED_REFERENCE!>shrek<!>.<!UNRESOLVED_REFERENCE!>brek<!> += 10
}
