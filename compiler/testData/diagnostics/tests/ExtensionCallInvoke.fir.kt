fun bar(doIt: Int.() -> Int) {
    1.<!UNRESOLVED_REFERENCE!>doIt<!>()
    1?.<!UNRESOLVED_REFERENCE!>doIt<!>()
    val i: Int? = 1
    i.<!UNRESOLVED_REFERENCE!>doIt<!>()
    i?.<!UNRESOLVED_REFERENCE!>doIt<!>()
}
