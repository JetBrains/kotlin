// !DUMP_CFG
fun String.check(): Boolean = true

fun test_1(s: String?) {
    if (s?.check() == true) {
        s.length // Should be OK
    } else {
        s.<!INAPPLICABLE_CANDIDATE!>length<!> // Should be bad
    }
}

fun test_2(s: String?) {
    if (s?.check() == false) {
        s.length // Should be OK
    } else {
        s.<!INAPPLICABLE_CANDIDATE!>length<!> // Should be bad
    }
}

fun test_3(s: String?) {
    if (s?.check() != true) {
        s.<!INAPPLICABLE_CANDIDATE!>length<!> // Should be bad
    } else {
        s.length // Should be OK
    }
}

fun test_4(s: String?) {
    if (s?.check() != false) {
        s.<!INAPPLICABLE_CANDIDATE!>length<!> // Should be bad
    } else {
        s.length // Should be OK
    }
}