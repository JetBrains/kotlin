fun foo(s: String) = s.length

fun baz(s: String?, r: String?): Int {
    return foo(<!DEBUG_INFO_SMARTCAST!>r ?: when {
        s != null -> s
        else -> ""
    }<!>)
}

fun bar(s: String?, r: String?): Int {
    return <!DEBUG_INFO_SMARTCAST!>(r ?: when {
        s != null -> s
        else -> ""
    })<!>.length
}