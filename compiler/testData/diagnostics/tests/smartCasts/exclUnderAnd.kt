class D(val a: String, val b: Boolean)

fun foo(p: Boolean, v: D?): String {
    if (p && v!!.b) <!DEBUG_INFO_SMARTCAST!>v<!>.a
    else v<!UNSAFE_CALL!>.<!>a
    if (p && v!! == D("?", false)) <!DEBUG_INFO_SMARTCAST!>v<!>.a
    else v<!UNSAFE_CALL!>.<!>a
    if (p || v!!.b) v<!UNSAFE_CALL!>.<!>a
    else <!DEBUG_INFO_SMARTCAST!>v<!>.a
    if (p || v!! == D("?", false)) v<!UNSAFE_CALL!>.<!>a
    else <!DEBUG_INFO_SMARTCAST!>v<!>.a
    return ""
} 
