// DIAGNOSTICS: -USELESS_ELVIS

fun test() {
    bar(<!TYPE_MISMATCH, TYPE_MISMATCH!>if (true) {
        1
    } else {
        2
    }<!>)

    bar(<!TYPE_MISMATCH, TYPE_MISMATCH!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> ?: <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!><!>)
}

fun bar(s: String) = s
