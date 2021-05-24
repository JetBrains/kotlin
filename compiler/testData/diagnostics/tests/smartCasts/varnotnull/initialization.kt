// FIR_IDENTICAL
fun foo() {
    var v: String? = "xyz"
    // It is possible in principle to provide smart cast here
    v<!UNSAFE_CALL!>.<!>length
    v = null
    v<!UNSAFE_CALL!>.<!>length
}
