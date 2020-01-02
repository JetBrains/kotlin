fun baz(s: String?): String {
    if (s == null) return ""
    // if explicit type String is given for t, problem disappears
    val t = when(s) {
        // !! is detected as unnecessary here
        "abc" -> s
        else -> "xyz"
    }
    return t
}

fun foo(s: String?): String {
    val t = when {
        s != null -> s
        else -> ""
    }
    return t
}
