// LANGUAGE: +ReadDeserializedContracts +UseReturnsEffect
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testIsNullOrBlank(x: String?) {
    if (x.isNullOrBlank()) {
        x<!UNSAFE_CALL!>.<!>length
    }
    else {
        x.length
    }
}

fun testIsNotNullOrBlank(x: String?) {
    if (!x.isNullOrBlank()) {
        x.length
    }

    x<!UNSAFE_CALL!>.<!>length
}

