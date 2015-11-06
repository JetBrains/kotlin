fun foo(): Int {
    var s: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>"abc"<!>
    s = null
    return <!ALWAYS_NULL!>s<!><!UNSAFE_CALL!>.<!>length
}