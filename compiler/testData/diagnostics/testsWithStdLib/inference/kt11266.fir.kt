// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

fun foo(first: Array<Any?>, second: Array<Any?>) = Pair(first.<!INAPPLICABLE_CANDIDATE!>toCollection<!>(), second.<!INAPPLICABLE_CANDIDATE!>toCollection<!>())
