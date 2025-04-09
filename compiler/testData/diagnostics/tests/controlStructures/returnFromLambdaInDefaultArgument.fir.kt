// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76660
fun foo(s: String = run { <!RETURN_NOT_ALLOWED!>return<!> "???" }) = s
