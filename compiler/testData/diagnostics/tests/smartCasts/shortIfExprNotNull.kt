fun baz(s: String?): String {
    val t = if (s != null) s
    else {
        val u: String? = null
        if (u == null) return ""
        u
    }
    return <!DEBUG_INFO_SMARTCAST!>t<!>
}
