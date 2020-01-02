fun baz(s: String?, b: Boolean?): String {
    val t = if (if (b == null) return "" else b) {
        if (s == null) return ""
        s
    }
    else {
        if (s != null) return s
        ""
    }
    return t
}