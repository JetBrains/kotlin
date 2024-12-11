// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// WITH_STDLIB

@JvmInline
inline value class A(val x: Int)

@JvmInline
value inline class B(val x: Int)

<!SYNTAX!>thisIsToMakeDiagnosticTestHaveDiagnostics<!>
