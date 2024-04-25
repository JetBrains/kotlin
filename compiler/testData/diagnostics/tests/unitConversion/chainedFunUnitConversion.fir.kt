// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun interface KRunnable {
    fun run()
}

fun foo(r: KRunnable) {}

abstract class SubInt : () -> Int

fun test(f: () -> Int, s: SubInt) {
    foo(<!ARGUMENT_TYPE_MISMATCH!>f<!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>s<!>)
}