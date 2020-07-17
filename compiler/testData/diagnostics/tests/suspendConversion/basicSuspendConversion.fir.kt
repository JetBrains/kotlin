// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo1(f: suspend () -> String) {}
fun foo2(f: suspend (Int) -> String) {}
fun foo3(f: suspend () -> Unit) {}

fun test(
    f0: suspend () -> String,
    f1: () -> String,
    f2: (Int) -> String,
    f3: () -> Unit,
) {
    foo1 { "str" }
    foo1(f0)

    <!INAPPLICABLE_CANDIDATE!>foo1<!>(f1)
    <!INAPPLICABLE_CANDIDATE!>foo2<!>(f2)
    <!INAPPLICABLE_CANDIDATE!>foo3<!>(f3)

    <!INAPPLICABLE_CANDIDATE!>foo1<!>(f2)
    <!INAPPLICABLE_CANDIDATE!>foo1<!>(f3)
}
