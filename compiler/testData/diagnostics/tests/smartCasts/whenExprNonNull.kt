fun baz(s: String?): String {
    if (s == null) return ""
    // if explicit type String is given for t, problem disappears
    val t = when(<!DEBUG_INFO_SMARTCAST!>s<!>) {
        // !! is detected as unnecessary here
        "abc" -> s
        else -> "xyz"
    }
    return <!DEBUG_INFO_SMARTCAST!>t<!>
}

fun foo(s: String?): String {
    val t = when {
        s != null -> s
        else -> ""
    }
    return <!DEBUG_INFO_SMARTCAST!>t<!>
}
