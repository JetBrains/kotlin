fun baz(s: String?): String {
    // If String type is given explicitly, problem disappears
    val t = if (s == null) {
        ""
    }
    else {
        val u: String? = null
        if (u == null) return ""
        // !! is detected as unnecessary here
        u
    }
    return <!DEBUG_INFO_SMARTCAST!>t<!>
}

fun foo(s: String?): String {
    if (s == null) return ""
    val t = if (s == "abc") <!DEBUG_INFO_SMARTCAST!>s<!> else "xyz"
    return t
}
