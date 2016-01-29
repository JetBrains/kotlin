fun baz(s: String?, u: String?): String {
    val t = when(if (u == null) return "" else <!DEBUG_INFO_SMARTCAST!>u<!>) {
        "abc" -> <!DEBUG_INFO_SMARTCAST!>u<!>
        "" -> {
            if (s == null) return ""
            <!DEBUG_INFO_SMARTCAST!>s<!>
        }
        else -> <!DEBUG_INFO_SMARTCAST!>u<!>
    }
    return t
}