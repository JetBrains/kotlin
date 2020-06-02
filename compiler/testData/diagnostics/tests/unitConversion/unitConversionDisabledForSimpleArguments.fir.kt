// !LANGUAGE: -SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun foo(f: () -> Unit) {}

fun bar(): Int = 0

abstract class SubInt : () -> Int

fun test(f: () -> String, s: SubInt) {
    foo { "lambda" }
    foo(::bar)

    <!INAPPLICABLE_CANDIDATE!>foo<!>(f)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(s)
}