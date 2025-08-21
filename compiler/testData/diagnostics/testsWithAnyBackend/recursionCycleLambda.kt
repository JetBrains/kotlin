// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

inline fun f(): Unit = <!INLINE_CALL_CYCLE!>g()<!>

inline fun g(): Unit = h { <!INLINE_CALL_CYCLE!>f()<!> }

inline fun h(fn: ()->Unit): Unit = fn()

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, lambdaLiteral */
