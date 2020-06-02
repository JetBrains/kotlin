// !LANGUAGE: +UnitConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun foo(f: (Int, String) -> Unit) {}

abstract class SubInt : (Int, String) -> Int
abstract class SubIntWrong : (String, String) -> Int

fun test1(s: SubInt, sWrong: SubIntWrong) {
    foo(s)
    foo(<!TYPE_MISMATCH!>sWrong<!>)

    val a = "foo"
    foo(<!TYPE_MISMATCH!>a<!>)

    a <!CAST_NEVER_SUCCEEDS!>as<!> (Int, String) -> String
    foo(a)
}

fun <T> test2(x: T) where T : (Int, String) -> Int, T : (Double) -> Int {
    foo(x)
}