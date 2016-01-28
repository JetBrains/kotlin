fun baz(s: String?): String {
    val t = if (s != null) <!DEBUG_INFO_SMARTCAST!>s<!>
    else {
        val u: String? = null
        if (u == null) return ""
        <!DEBUG_INFO_SMARTCAST!>u<!>
    }
    return t
}
