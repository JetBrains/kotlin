fun baz(s: String?): String {
    val t = if (s == null) {
        ""
    }
    else if (s == "") {
        val u: String? = null
        if (u == null) return ""
        u
    }
    else {
        s
    }
    return t
}
