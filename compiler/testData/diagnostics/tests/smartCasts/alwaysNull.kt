fun foo(): String {
    var s: String?
    s = null
    <!ALWAYS_NULL!>s<!>?.length
    if (<!ALWAYS_NULL!>s<!> == null) s = "z"
    var t: String? = "y"
    if (t == null) t = "x"
    var x: Int? = null
    if (x == null) <!TYPE_MISMATCH!>x += null<!>
    return <!DEBUG_INFO_SMARTCAST!>t<!> + s
}