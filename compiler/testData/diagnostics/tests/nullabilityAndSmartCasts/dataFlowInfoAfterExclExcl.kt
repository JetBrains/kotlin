// !WITH_NEW_INFERENCE

fun foo(d: Any?) {
    if (d is String?) {
        <!DEBUG_INFO_SMARTCAST!>d<!>!!
        doString(<!NI;TYPE_MISMATCH, OI;DEBUG_INFO_SMARTCAST!>d<!>)
    }
}

fun doString(s: String) = s