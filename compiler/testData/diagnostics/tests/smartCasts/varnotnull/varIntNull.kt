// !WITH_NEW_INFERENCE
fun foo(): Int {
    var i: Int? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>42<!>
    i = null
    return <!NI;TYPE_MISMATCH, TYPE_MISMATCH!><!OI;DEBUG_INFO_CONSTANT!>i<!> + 1<!>
}