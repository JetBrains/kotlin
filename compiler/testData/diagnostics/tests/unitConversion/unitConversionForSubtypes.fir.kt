// !LANGUAGE: +UnitConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun foo(f: (Int, String) -> Unit) {}

abstract class SubInt : (Int, String) -> Int
abstract class SubIntWrong : (String, String) -> Int

fun test1(s: SubInt, sWrong: SubIntWrong) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(s)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(sWrong)

    val a = "foo"
    <!INAPPLICABLE_CANDIDATE!>foo<!>(a)

    a as (Int, String) -> String
    <!INAPPLICABLE_CANDIDATE!>foo<!>(a)
}

fun <T> test2(x: T) where T : (Int, String) -> Int, T : (Double) -> Int {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(x)
}