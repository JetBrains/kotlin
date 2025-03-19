// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-72335
// FIR_DUMP

fun foo(b: Boolean, block: (Int.() -> Unit)) {
    block(1.<!ARGUMENT_TYPE_MISMATCH, ILLEGAL_SELECTOR!>{ if (b) "s1" else "s2" }<!>)
}
