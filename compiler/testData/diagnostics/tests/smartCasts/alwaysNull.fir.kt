fun foo(): String {
    var s: String?
    s = null
    s?.length
    s<!UNSAFE_CALL!>.<!>length
    if (<!SENSELESS_COMPARISON!>s == null<!>) return s!!
    var t: String? = "y"
    if (t == null) t = "x"
    var x: Int? = null
    if (x == null) <!ASSIGNMENT_TYPE_MISMATCH!>x += null<!>
    return t + s
}

fun String?.gav() {}

fun bar(s: String?) {
    if (s != null) return
    s.gav()
    s <!USELESS_CAST!>as? String<!>
    s as String?
    s <!CAST_NEVER_SUCCEEDS!>as<!> String
}
