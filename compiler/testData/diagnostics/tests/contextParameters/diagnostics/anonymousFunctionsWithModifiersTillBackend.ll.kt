// LL_FIR_DIVERGENCE
// KT-87442: TODO: Add IrDiagnosticsHandler to lowered IR handlers, to emit IR diagnostics like NO_TAIL_CALLS_FOUND
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextParameters
val t2 = context(a: String) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun() {}

/* GENERATED_FIR_TAGS: anonymousFunction, propertyDeclaration */
