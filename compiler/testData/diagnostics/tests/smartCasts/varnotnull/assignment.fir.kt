fun foo() {
    var v: String? = null
    v<!UNSAFE_CALL!>.<!>length
    v = "abc"
    v.length
    v = null
    v<!UNSAFE_CALL!>.<!>length
    v = "abc"
    v.length
}
