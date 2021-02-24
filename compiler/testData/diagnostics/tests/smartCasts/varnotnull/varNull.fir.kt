// !WITH_NEW_INFERENCE
fun foo(): Int {
    var s: String? = "abc"
    s = null
    return s<!UNSAFE_CALL!>.<!>length
}
