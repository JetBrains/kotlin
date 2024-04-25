// LANGUAGE: +ReadDeserializedContracts +UseReturnsEffect
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testIsNullOrEmpty(x: String?) {
    if (x.isNullOrEmpty()) {
        x<!UNSAFE_CALL!>.<!>length
    }
    else {
        x.length
    }
}

fun testIsNotNullOrEmpty(x: String?) {
    if (!x.isNullOrEmpty()) {
        x.length
    }

    x<!UNSAFE_CALL!>.<!>length
}

