// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(f: () -> String, g: suspend () -> String, h: suspend () -> String) {}

fun test(f: () -> String, g: suspend () -> String) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(f, f, f)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(f, { "str" }, f)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(f, f, g)
    foo(f, g, g)
}
