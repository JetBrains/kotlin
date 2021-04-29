// !WITH_NEW_INFERENCE
fun foo(): String {
    var s: String?
    s = null
    s?.length
    s<!UNSAFE_CALL!>.<!>length
    if (s == null) return s!!
    var t: String? = "y"
    if (t == null) t = "x"
    var x: Int? = null
    if (x == null) x <!UNRESOLVED_REFERENCE!>+=<!> null
    return t + s
}

fun String?.gav() {}

fun bar(s: String?) {
    if (s != null) return
    s.gav()
    s <!USELESS_CAST!>as? String<!>
    s <!USELESS_CAST!>as String?<!>
    s as String
}
