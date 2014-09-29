fun foo(d: Any?) {
    if (d is String?) {
        <!DEBUG_INFO_SMARTCAST!>d<!>!!
        doString(<!DEBUG_INFO_SMARTCAST!>d<!>)
    }
}

fun doString(s: String) = s