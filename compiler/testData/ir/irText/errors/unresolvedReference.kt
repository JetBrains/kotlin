// IGNORE_ERRORS
// IGNORE_FIR_DIAGNOSTICS
// DIAGNOSTICS: -UNRESOLVED_REFERENCE -OVERLOAD_RESOLUTION_AMBIGUITY
// IGNORE_BACKEND_K1: JS_IR
// SKIP_KLIB_TEST

val test1 = unresolved

val test2: Unresolved =
        unresolved()

val test3 = 42.unresolved(56)
