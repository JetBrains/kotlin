fun foo(s: String) = s.length

fun baz(s: String?, r: String?): Int {
    return foo(r ?: when {
        s != null -> <!DEBUG_INFO_SMARTCAST!>s<!>
        else -> ""
    })
}

fun bar(s: String?, r: String?): Int {
    return (r ?: when {
        s != null -> <!DEBUG_INFO_SMARTCAST!>s<!>
        else -> ""
    }).length
}