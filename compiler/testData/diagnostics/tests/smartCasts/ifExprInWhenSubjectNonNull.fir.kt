fun baz(s: String?, u: String?): String {
    val t = when(if (u == null) return "" else u) {
        "abc" -> u
        "" -> {
            if (s == null) return ""
            s
        }
        else -> u
    }
    return t
}