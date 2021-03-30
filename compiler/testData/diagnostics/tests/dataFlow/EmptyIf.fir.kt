// !CHECK_TYPE

fun f1(s: String?) {
    if (s!! == "");
    checkSubtype<String>(s)
}

fun f2(s: Number?) {
    if (s is Int);
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(s)
    if (s as Int == 42);
    checkSubtype<Int>(s)
}

fun f3(s: Number?) {
    if (s is Int && s as Int == 42);
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(s)
}

fun f4(s: Int?) {
    var u = <!INVALID_IF_AS_EXPRESSION!>if<!> (s!! == 42);
    if (u == Unit) u = <!INVALID_IF_AS_EXPRESSION!>if<!> (s == 239);
    return u
}
