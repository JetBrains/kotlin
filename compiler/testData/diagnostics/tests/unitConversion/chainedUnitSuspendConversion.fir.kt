// !LANGUAGE: +UnitConversion +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun foo(f: suspend () -> Unit) {}

fun bar(): String = ""

abstract class SubInt : () -> Int

fun test(g: () -> Double, s: SubInt) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(::bar)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(g)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(s)
}