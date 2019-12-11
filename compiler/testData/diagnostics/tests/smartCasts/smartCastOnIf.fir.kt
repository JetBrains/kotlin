fun baz(s: String?): Int {
    return if (s == null) {
        ""
    }
    else {
        val u: String? = null
        if (u == null) return 0
        u
    }.length
}
