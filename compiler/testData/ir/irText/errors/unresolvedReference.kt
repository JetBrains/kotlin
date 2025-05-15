// IGNORE_ERRORS
// IGNORE_FIR_DIAGNOSTICS
// DIAGNOSTICS: -UNRESOLVED_REFERENCE -OVERLOAD_RESOLUTION_AMBIGUITY
// IGNORE_BACKEND: JS_IR NATIVE
// REASON: Serialization of IrErrorType is not supported anymore
// SKIP_GENERATING_KLIB
// REASON: Cannot serialize error type: ERROR CLASS: Unresolved name: unresolved
// RUN_PIPELINE_TILL: FIR2IR

val test1 = unresolved

val test2: Unresolved =
        unresolved()

val test3 = 42.unresolved(56)
