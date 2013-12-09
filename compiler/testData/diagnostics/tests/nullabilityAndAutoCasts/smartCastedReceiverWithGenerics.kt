fun test(a: Any?) {
    if (a != null) {
        <!DEBUG_INFO_AUTOCAST!>a<!>.foo(11)
    }
}

fun <T> Any.foo(t: T) = t