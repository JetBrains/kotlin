fun testIsNullOrEmpty(x: String?) {
    if (x.isNullOrEmpty()) {
        x<!UNSAFE_CALL!>.<!>length
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun testIsNotNullOrEmpty(x: String?) {
    if (!x.isNullOrEmpty()) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }

    x<!UNSAFE_CALL!>.<!>length
}

