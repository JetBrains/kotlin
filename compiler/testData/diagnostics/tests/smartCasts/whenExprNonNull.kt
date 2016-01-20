fun baz(s: String?): String {
    if (s == null) return ""
    // if explicit type String is given for t, problem disappears
    val t = when(<!DEBUG_INFO_SMARTCAST!>s<!>) {
        // !! is detected as unnecessary here
        "abc" -> <!DEBUG_INFO_SMARTCAST!>s<!>
        else -> "xyz"
    }
    return t
}

fun foo(s: String?): String {
    val t = when {
        s != null -> <!DEBUG_INFO_SMARTCAST!>s<!>
        else -> ""
    }
    return t
}
