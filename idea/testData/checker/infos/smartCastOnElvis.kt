fun foo(s: String) = s.length

fun baz(s: String?, r: String?): Int {
    return foo(r ?: when {
        s != null -> <info descr="Smart cast to kotlin.String">s</info>
        else -> ""
    })
}

fun bar(s: String?, r: String?): Int {
    return (r ?: when {
        s != null -> <info descr="Smart cast to kotlin.String">s</info>
        else -> ""
    }).length
}