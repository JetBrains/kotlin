fun f1(s: String?) {
    if (s!! == "");
    <!DEBUG_INFO_AUTOCAST!>s<!> : String
}

fun f2(s: Number?) {
    if (s is Int);
    <!TYPE_MISMATCH!>s<!> : Int
    if (s as Int == 42);
    <!DEBUG_INFO_AUTOCAST!>s<!> : Int
}

fun f3(s: Number?) {
    if (s is Int && s as Int == 42);
    <!TYPE_MISMATCH!>s<!> : Int
}

fun f4(s: Int?) {
    var u = <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>if (s!! == 42)<!>;
    if (u == Unit) u = if (s == 239);
    return u
}
