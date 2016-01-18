fun foo(s: Any): String {
    val x = when (s) {
        is String -> s
        is Int -> "$s"
        else -> return ""
    }

    val y: String = <!DEBUG_INFO_SMARTCAST!>x<!> // should be Ok
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