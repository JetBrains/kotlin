// !WITH_NEW_INFERENCE
fun foo(): String {
    var s: String?
    s = null
    s?.length
    s.<!INAPPLICABLE_CANDIDATE!>length<!>
    if (s == null) return s!!
    var t: String? = "y"
    if (t == null) t = "x"
    var x: Int? = null
    if (x == null) <!UNRESOLVED_REFERENCE!>x += null<!>
    return t + s
}

fun String?.gav() {}

fun bar(s: String?) {
    if (s != null) return
    s.gav()
    s as? String
    s as String?
    s as String
}