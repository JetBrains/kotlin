// IGNORE_ERRORS
// IGNORE_FIR_DIAGNOSTICS
// DIAGNOSTICS: -UNRESOLVED_REFERENCE -OVERLOAD_RESOLUTION_AMBIGUITY
// ERROR_POLICY: SEMANTIC
// SKIP_KLIB_TEST

val test1 = unresolved

val test2: Unresolved =
        unresolved()

val test3 = 42.unresolved(56)
