fun foo(): Int {
    var s: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>"abc"<!>
    s = null
    return s<!UNSAFE_CALL!>.<!>length
}
