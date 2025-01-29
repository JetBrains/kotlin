// IGNORE_ERRORS
// IGNORE_FIR_DIAGNOSTICS
// DIAGNOSTICS: -UNRESOLVED_REFERENCE -OVERLOAD_RESOLUTION_AMBIGUITY
// IGNORE_BACKEND_K1: JS_IR
// SKIP_GENERATING_KLIB
// REASON: Cannot serialize error type: ERROR CLASS: Unresolved name: unresolved
// RUN_PIPELINE_TILL: FIR2IR

val test1 = unresolved

val test2: Unresolved =
        unresolved()

val test3 = 42.unresolved(56)
