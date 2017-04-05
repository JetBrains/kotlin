fun <T : Any?> foo(x: T) {
    if (x is String?) {
        x<!UNSAFE_CALL!>.<!>length

        if (x != null) {
            <!DEBUG_INFO_SMARTCAST!>x<!>.length
        }
    }

    if (x is String) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}
