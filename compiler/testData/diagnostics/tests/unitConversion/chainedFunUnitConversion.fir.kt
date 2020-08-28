// !LANGUAGE: +UnitConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun interface KRunnable {
    fun run()
}

fun foo(r: KRunnable) {}

abstract class SubInt : () -> Int

fun test(f: () -> Int, s: SubInt) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(f)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(s)
}