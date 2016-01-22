fun foo(s: Any): String {
    val x = when (s) {
        is String -> <!DEBUG_INFO_SMARTCAST!>s<!>
        is Int -> "$s"
        else -> return ""
    }

    val y: String = x // should be Ok
    return y
}

fun bar(s: Any): String {
    val x = when (s) {
        is String -> s <!USELESS_CAST!>as String<!> // meaningless
        is Int -> "$s"
        else -> return ""
    }

    val y: String = x // no error
    return y
}