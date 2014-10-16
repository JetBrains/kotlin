fun test(a: Any?) {
    if (a != null) {
        <!DEBUG_INFO_SMARTCAST!>a<!>.foo(11)
    }
}

fun <T> Any.foo(t: T) = t