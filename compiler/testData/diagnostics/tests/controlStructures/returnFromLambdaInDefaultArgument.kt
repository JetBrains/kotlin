// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-76660
fun foo(s: String = run { return "???" }) = s