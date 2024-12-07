// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +UnitConversionsOnArbitraryExpressions +SuspendConversion
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun interface SuspendRunnable {
    suspend fun run()
}

fun foo(r: SuspendRunnable) {}

fun bar(): String = ""

abstract class SubInt : () -> Int

fun test(f: () -> String, s: SubInt) {
    foo(<!ARGUMENT_TYPE_MISMATCH!>f<!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>s<!>)
    foo(::bar)
}
