// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -USELESS_ELVIS

fun test() {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(if (true) {
        1
    } else {
        2
    })

    <!INAPPLICABLE_CANDIDATE!>bar<!>(1 ?: 2)
}

fun bar(s: String) = s