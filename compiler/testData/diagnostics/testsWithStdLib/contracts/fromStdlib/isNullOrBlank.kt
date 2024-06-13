// LANGUAGE: +ReadDeserializedContracts +UseReturnsEffect
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testIsNullOrBlank(x: String?) {
    if (x.isNullOrBlank()) {
        x<!UNSAFE_CALL!>.<!>length
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun testIsNotNullOrBlank(x: String?) {
    if (!x.isNullOrBlank()) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }

    x<!UNSAFE_CALL!>.<!>length
}

