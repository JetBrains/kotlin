fun baz(s: String?, u: String?): String {
    val t = when(s) {
        is String -> {
            if (u == null) return <!DEBUG_INFO_SMARTCAST!>s<!>
            <!DEBUG_INFO_SMARTCAST!>u<!>
        }
        else -> {
            if (u == null) return ""
            <!DEBUG_INFO_SMARTCAST!>u<!>
        }
    }
    return t
}