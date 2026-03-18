// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-18967

// KT-18967: Visibility error is reported twice on annotations
@kotlin.internal.<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>InlineOnly<!>
inline fun foo() {}

/* GENERATED_FIR_TAGS: functionDeclaration, inline */
