// !WITH_NEW_INFERENCE
fun foo(): Int {
    var s: String? = "abc"
    s = null
    return s.<!INAPPLICABLE_CANDIDATE!>length<!>
}