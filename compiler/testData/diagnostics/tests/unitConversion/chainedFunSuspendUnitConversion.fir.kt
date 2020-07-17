// !LANGUAGE: +UnitConversion +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun interface SuspendRunnable {
    suspend fun run()
}

fun foo(r: SuspendRunnable) {}

fun bar(): String = ""

abstract class SubInt : () -> Int

fun test(f: () -> String, s: SubInt) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(f)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(s)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(::bar)
}