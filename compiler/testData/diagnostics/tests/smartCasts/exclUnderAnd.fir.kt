class D(val a: String, val b: Boolean)

fun foo(p: Boolean, v: D?): String {
    if (p && v!!.b) v.a
    else v.<!INAPPLICABLE_CANDIDATE!>a<!>
    if (p && v!! == D("?", false)) v.a
    else v.<!INAPPLICABLE_CANDIDATE!>a<!>
    if (p || v!!.b) v.<!INAPPLICABLE_CANDIDATE!>a<!>
    else v.a
    if (p || v!! == D("?", false)) v.<!INAPPLICABLE_CANDIDATE!>a<!>
    else v.a
    return ""
} 
