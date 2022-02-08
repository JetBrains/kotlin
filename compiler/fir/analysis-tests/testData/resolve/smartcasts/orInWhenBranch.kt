// ISSUE: KT-36057

fun String.foo() {}

fun test_1(a: Any?) {
    when (a) {
        is String, is Any -> a.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // Should be Bad
    }
}

fun test_2(a: Any?) {
    if (a is String || a is Any) {
        a.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // Should be Bad
    }
}

fun test_3(a: Any?, b: Boolean) {
    when (a) {
        is String, b -> a.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // Should be Bad
    }
}

fun test_4(a: Any?, b: Boolean) {
    if (a is String || b) {
        a.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // Should be Bad
    }
}
