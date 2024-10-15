// RUN_PIPELINE_TILL: SOURCE
// DIAGNOSTICS: -USELESS_ELVIS

fun test() {
    bar(<!ARGUMENT_TYPE_MISMATCH!>if (true) {
        1
    } else {
        2
    }<!>)

    bar(<!ARGUMENT_TYPE_MISMATCH!>1 ?: 2<!>)
}

fun bar(s: String) = s
