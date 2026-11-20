// LL_FIR_DIVERGENCE
// FIR checkers don't raise backend's INLINE_CALL_CYCLE diagnostic.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun foo(x: Int = bar()): Int = x

inline fun bar(x: Int = foo()): Int = x

inline fun qux(x: Int = quz(42)): Int = x

inline fun quz(x: Int = qux(42)): Int = x

/* GENERATED_FIR_TAGS: functionDeclaration, inline, integerLiteral */
