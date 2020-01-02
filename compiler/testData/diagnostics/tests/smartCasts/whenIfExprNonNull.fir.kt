fun baz(s: String?, u: String?): String {
    val t = when(s) {
        is String -> {
            if (u == null) return s
            u
        }
        else -> {
            if (u == null) return ""
            u
        }
    }
    return t
}