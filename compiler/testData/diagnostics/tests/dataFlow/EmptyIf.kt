// !CHECK_TYPE

fun f1(s: String?) {
    if (s!! == "");
    checkSubtype<String>(<!DEBUG_INFO_SMARTCAST!>s<!>)
}

fun f2(s: Number?) {
    if (s is Int);
    checkSubtype<Int>(<!TYPE_MISMATCH!>s<!>)
    if (s as Int == 42);
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>s<!>)
}

fun f3(s: Number?) {
    if (s is Int && s <!USELESS_CAST!>as Int<!> == 42);
    checkSubtype<Int>(<!TYPE_MISMATCH!>s<!>)
}

fun f4(s: Int?) {
    var u = <!INVALID_IF_AS_EXPRESSION!>if<!> (s!! == 42);
    if (u == Unit) u = <!INVALID_IF_AS_EXPRESSION!>if<!> (s == 239);
    return u
}