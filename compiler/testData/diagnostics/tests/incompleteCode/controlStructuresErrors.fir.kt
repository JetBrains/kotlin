// !WITH_NEW_INFERENCE

fun test1() {
    if (<!UNRESOLVED_REFERENCE!>rr<!>) {
        if (<!UNRESOLVED_REFERENCE!>l<!>) {
            <!UNRESOLVED_REFERENCE!>a<!>.<!UNRESOLVED_REFERENCE!>q<!>()
        }
        else {
            <!UNRESOLVED_REFERENCE!>a<!>.<!UNRESOLVED_REFERENCE!>w<!>()
        }
    }
    else {
        if (<!UNRESOLVED_REFERENCE!>n<!>) {
            <!UNRESOLVED_REFERENCE!>a<!>.<!UNRESOLVED_REFERENCE!>t<!>()
        }
        else {
            <!UNRESOLVED_REFERENCE!>a<!>.<!UNRESOLVED_REFERENCE!>u<!>()
        }
    }
}

fun test2(l: List<<!UNRESOLVED_REFERENCE!>AA<!>>) {
    l.<!UNRESOLVED_REFERENCE!>map<!> {
        <!UNRESOLVED_REFERENCE!>it<!>!!
    }
}
