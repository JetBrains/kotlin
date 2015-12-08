fun foo(s: String) = s.length

fun baz(s: String?, r: String?): Int {
    return foo(r <info descr="Smart cast to kotlin.String">?:</info> when {
        s != null -> s
        else -> ""
    })
}

fun bar(s: String?, r: String?): Int {
    return (r <info descr="Smart cast to kotlin.String">?:</info> when {
        s != null -> s
        else -> ""
    }).length
}