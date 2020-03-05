// !WITH_NEW_INFERENCE
fun foo() {
    var v: String? = null
    v.<!INAPPLICABLE_CANDIDATE!>length<!>
    v = "abc"
    v.length
    v = null
    v.<!INAPPLICABLE_CANDIDATE!>length<!>
    v = "abc"
    v.length
}