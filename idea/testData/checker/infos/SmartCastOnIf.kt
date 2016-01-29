fun baz(s: String?): Int {
    return if (s == null) {
        ""
    }
    else {
        val u: String? = null
        if (u == null) return 0
        <info descr="Smart cast to kotlin.String">u</info>
    }.length
}
