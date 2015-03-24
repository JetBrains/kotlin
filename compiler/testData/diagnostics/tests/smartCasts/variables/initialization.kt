fun foo() {
    var v: Any = "xyz"
    // It is possible in principle to provide smart cast here
    // but now we decide that v is Any
    v.<!UNRESOLVED_REFERENCE!>length<!>()
    v = 42
    v.<!UNRESOLVED_REFERENCE!>length<!>()
}