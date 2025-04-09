// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76660
fun foo(s: String = run { return "???" }) = s