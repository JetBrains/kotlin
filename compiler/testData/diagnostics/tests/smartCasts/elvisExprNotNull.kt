fun foo(s: Any?): String {
    val t = when {
        // To resolve: String U Nothing? = String?
        s is String -> s
        else -> null
    } ?: ""
    // Ideally we should have smart cast to String here
    return <!TYPE_MISMATCH!>t<!>
}

fun bar(s: Any?): String {
    // To resolve: String U Nothing? = String?
    val t = (if (s == null) {
        null
    }
    else {
        val u: Any? = null
        if (u !is String) return ""
        u
    }) ?: "xyz"
    // Ideally we should have smart cast to String here
    return <!TYPE_MISMATCH!>t<!>
}

fun baz(s: String?, r: String?): String {
    val t = r ?: when {
        s != null -> s
        else -> ""
    }
    return <!DEBUG_INFO_SMARTCAST!>t<!>
}

fun withNull(s: String?): String {
    val t = s ?: null
    // Error: nullable
    return <!TYPE_MISMATCH!>t<!>
}