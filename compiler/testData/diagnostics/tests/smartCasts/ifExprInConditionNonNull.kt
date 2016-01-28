fun baz(s: String?, b: Boolean?): String {
    val t = if (if (b == null) return "" else <!DEBUG_INFO_SMARTCAST!>b<!>) {
        if (s == null) return ""
        <!DEBUG_INFO_SMARTCAST!>s<!>
    }
    else {
        if (s != null) return <!DEBUG_INFO_SMARTCAST!>s<!>
        ""
    }
    return t
}