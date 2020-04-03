// !WITH_NEW_INFERENCE
fun foo(): String {
    var s: String?
    s = null
    <!DEBUG_INFO_CONSTANT!>s<!>?.length
    <!OI;DEBUG_INFO_CONSTANT!>s<!><!UNSAFE_CALL!>.<!>length
    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> == null<!>) <!NI;UNREACHABLE_CODE!>return<!> <!ALWAYS_NULL!>s<!>!!
    var t: String? = "y"
    if (t == null) t = "x"
    var x: Int? = null
    if (x == null) <!TYPE_MISMATCH!><!OI;DEBUG_INFO_CONSTANT!>x<!> += null<!>
    return <!DEBUG_INFO_SMARTCAST!>t<!> + s
}

fun String?.gav() {}

fun bar(s: String?) {
    if (s != null) return
    <!OI;DEBUG_INFO_CONSTANT!>s<!>.gav()
    <!DEBUG_INFO_CONSTANT!>s<!> <!USELESS_CAST!>as? String<!>
    <!DEBUG_INFO_CONSTANT!>s<!> <!USELESS_CAST!>as String?<!>
    <!ALWAYS_NULL!>s<!> as String
}