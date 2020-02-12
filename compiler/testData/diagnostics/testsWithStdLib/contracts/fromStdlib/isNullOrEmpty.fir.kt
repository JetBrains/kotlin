// !LANGUAGE: +ReadDeserializedContracts +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testIsNullOrEmpty(x: String?) {
    if (x.isNullOrEmpty()) {
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    else {
        x.length
    }
}

fun testIsNotNullOrEmpty(x: String?) {
    if (!x.isNullOrEmpty()) {
        x.length
    }

    x.<!INAPPLICABLE_CANDIDATE!>length<!>
}

