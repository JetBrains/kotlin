fun test_1(s: String?) {
    when {
        !s.isNullOrEmpty() -> s.length // Should be OK
    }
}

fun test_2(s: String?) {
    // contracts related
    if (s.isNullOrEmpty()) {
        s.<!INAPPLICABLE_CANDIDATE!>length<!> // Should be bad
    } else {
        s.length // Should be OK
    }
}
