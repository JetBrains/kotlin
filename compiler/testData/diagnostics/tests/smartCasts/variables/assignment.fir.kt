fun foo() {
    var v: Any = 42
    v.<!UNRESOLVED_REFERENCE!>length<!>()
    v = "abc"
    v.length
    v = 42
    v.<!UNRESOLVED_REFERENCE!>length<!>()
    v = "abc"
    v.length
}