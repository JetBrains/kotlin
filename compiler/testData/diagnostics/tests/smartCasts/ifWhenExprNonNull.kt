fun baz(s: String?): String {
    val t = if (s == null) {
        ""
    }
    else {
        val u: String? = null
        when (u) {
            null -> ""
            else -> <!DEBUG_INFO_SMARTCAST!>u<!>
        }
    }
    return t
}
