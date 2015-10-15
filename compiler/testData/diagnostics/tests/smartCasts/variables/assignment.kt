fun foo() {
    var v: Any = 42
    v.<!UNRESOLVED_REFERENCE!>length<!>()
    v = "abc"
    <!DEBUG_INFO_SMARTCAST!>v<!>.length
    v = 42
    v.<!UNRESOLVED_REFERENCE!>length<!>()
    v = "abc"
    <!DEBUG_INFO_SMARTCAST!>v<!>.length
}