// KT-78882
// `getProgressionLastElement` was once part of the JVM builtins and also a regular function in the stdlib (prior to roughly 1.8.0), but now
// it is only a regular stdlib function. To avoid duplicates from the two declarations, symbol providers excluded the non-builtin version of
// the function. However, this exclusion led to `resolveToFirSymbol` and especially `getClassLikeSymbolByPsi` not finding a candidate for a
// given PSI function. Since it's not a builtin anymore, the exclusion was removed.

// WITH_STDLIB
// MODULE: main
// FILE: main.kt

// callable: kotlin/internal/getProgressionLastElement
