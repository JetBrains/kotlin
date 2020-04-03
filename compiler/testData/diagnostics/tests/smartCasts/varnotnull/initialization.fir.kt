// !WITH_NEW_INFERENCE
fun foo() {
    var v: String? = "xyz"
    // It is possible in principle to provide smart cast here
    v.<!INAPPLICABLE_CANDIDATE!>length<!>
    v = null
    v.<!INAPPLICABLE_CANDIDATE!>length<!>
}