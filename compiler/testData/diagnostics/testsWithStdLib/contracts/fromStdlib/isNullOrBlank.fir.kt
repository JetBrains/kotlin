// !LANGUAGE: +ReadDeserializedContracts +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testIsNullOrBlank(x: String?) {
    if (x.isNullOrBlank()) {
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    else {
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}

fun testIsNotNullOrBlank(x: String?) {
    if (!x.isNullOrBlank()) {
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
    }

    x.<!INAPPLICABLE_CANDIDATE!>length<!>
}

