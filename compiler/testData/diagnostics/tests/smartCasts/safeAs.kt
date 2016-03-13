// See also KT-10992: we should have no errors for all unsafe hashCode() calls

fun foo(arg: Any?) {
    val x = arg as? Any ?: return
    <!DEBUG_INFO_SMARTCAST!>arg<!>.hashCode()
    x.hashCode()
}

fun bar(arg: Any?) {
    arg as? Any ?: return
    <!DEBUG_INFO_SMARTCAST!>arg<!>.hashCode()
}

fun gav(arg: Any?) {
    arg as? String ?: return
    <!DEBUG_INFO_SMARTCAST!>arg<!>.length
}
