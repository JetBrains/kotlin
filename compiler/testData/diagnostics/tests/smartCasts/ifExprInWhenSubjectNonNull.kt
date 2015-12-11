fun baz(s: String?, u: String?): String {
    val t = when(if (u == null) return "" else <!DEBUG_INFO_SMARTCAST!>u<!>) {
        "abc" -> u
        "" -> {
            if (s == null) return ""
            s
        }
        else -> u
    }
    return <!DEBUG_INFO_SMARTCAST!>t<!>
}