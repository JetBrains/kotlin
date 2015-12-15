fun baz(s: String?, u: String?): String {
    val t = when(s) {
        is String -> {
            if (u == null) return <!DEBUG_INFO_SMARTCAST!>s<!>
            u
        }
        else -> {
            if (u == null) return ""
            u
        }
    }
    return <!DEBUG_INFO_SMARTCAST!>t<!>
}