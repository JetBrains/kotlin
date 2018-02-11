// !WITH_NEW_INFERENCE
fun foo(): Int {
    var s: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>"abc"<!>
    s = null
    return <!OI;DEBUG_INFO_CONSTANT!>s<!><!UNSAFE_CALL!>.<!>length
}