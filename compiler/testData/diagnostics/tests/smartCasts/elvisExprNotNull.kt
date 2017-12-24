// !WITH_NEW_INFERENCE
fun foo(s: Any?): String {
    val t = when {
        // To resolve: String U Nothing? = String?
        s is String -> <!DEBUG_INFO_SMARTCAST!>s<!>
        else -> null
    } ?: ""
    return t
}

fun bar(s: Any?): String {
    // To resolve: String U Nothing? = String?
    val t = (if (s == null) {
        null
    }
    else {
        val u: Any? = null
        if (u !is String) return ""
        <!DEBUG_INFO_SMARTCAST!>u<!>
    }) ?: "xyz"
    // Ideally we should have smart cast to String here
    return t
}

fun baz(s: String?, r: String?): String {
    val t = r ?: when {
        s != null -> <!DEBUG_INFO_SMARTCAST!>s<!>
        else -> ""
    }
    return t
}

fun withNull(s: String?): String {
    val t = s <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>
    // Error: nullable
    return <!TYPE_MISMATCH!>t<!>
}