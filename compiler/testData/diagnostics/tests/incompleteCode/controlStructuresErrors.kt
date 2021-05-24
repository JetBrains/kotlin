fun test1() {
    if (<!UNRESOLVED_REFERENCE!>rr<!>) {
        if (<!UNRESOLVED_REFERENCE!>l<!>) {
            <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>q<!>()
        }
        else {
            <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>w<!>()
        }
    }
    else {
        if (<!UNRESOLVED_REFERENCE!>n<!>) {
            <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>t<!>()
        }
        else {
            <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>u<!>()
        }
    }
}

fun test2(l: List<<!UNRESOLVED_REFERENCE!>AA<!>>) {
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>l<!>.<!UNRESOLVED_REFERENCE!>map<!> {
        <!UNRESOLVED_REFERENCE!>it<!>!!
    }
}
