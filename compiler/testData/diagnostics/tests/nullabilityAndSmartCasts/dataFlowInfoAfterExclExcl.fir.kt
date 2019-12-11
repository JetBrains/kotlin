fun foo(d: Any?) {
    if (d is String?) {
        d!!
        doString(d)
    }
}

fun doString(s: String) = s