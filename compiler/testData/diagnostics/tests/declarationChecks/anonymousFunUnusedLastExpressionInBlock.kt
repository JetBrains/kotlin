// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_EXPRESSION
// WITH_EXTRA_CHECKERS

fun unusedExpressions() {
    if (1 == 1)
        <!UNUSED_EXPRESSION!>fun(): Int {return 1}<!>
    else
        <!UNUSED_EXPRESSION!>fun() = 1<!>

    if (1 == 1) {
        <!UNUSED_EXPRESSION!>fun(): Int {
            return 1
        }<!>
    }
    else
        <!UNUSED_EXPRESSION!>fun() = 1<!>

    when (1) {
        0 -> <!UNUSED_EXPRESSION!>fun(): Int {return 1}<!>
        else -> <!UNUSED_EXPRESSION!>fun() = 1<!>
    }

    <!UNUSED_EXPRESSION!>fun() = 1<!>
}
