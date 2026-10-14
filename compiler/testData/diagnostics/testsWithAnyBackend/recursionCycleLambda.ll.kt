// LL_FIR_DIVERGENCE
// FIR checkers don't raise backend's INLINE_CALL_CYCLE diagnostic.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -NOTHING_TO_INLINE
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

inline fun f(): Unit = g()

inline fun g(): Unit = h { f() }

inline fun h(fn: ()->Unit): Unit = fn()

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, lambdaLiteral */
