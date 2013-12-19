fun foo(d: Any?) {
    if (d is String?) {
        <!DEBUG_INFO_AUTOCAST!>d<!>!!
        doString(<!DEBUG_INFO_AUTOCAST!>d<!>)
    }
}

fun doString(s: String) = s