fun baz(s: String?): Int {
    if (s == null) return 0
    return <info descr="Smart cast to kotlin.String">when</info>(<info descr="Smart cast to kotlin.String">s</info>) {
        "abc" -> s
        else -> "xyz"
    }.length
}
