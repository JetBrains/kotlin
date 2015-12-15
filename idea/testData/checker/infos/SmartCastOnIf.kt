fun baz(s: String?): Int {
    return <info descr="Smart cast to kotlin.String">if</info> (s == null) {
        ""
    }
    else {
        val u: String? = null
        if (u == null) return 0
        u
    }.length
}
