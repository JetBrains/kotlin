// LL_FIR_DIVERGENCE
// FIR checkers don't raise backend's INLINE_CALL_CYCLE diagnostic.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

inline val String.foo: String
    get() = foo

/* GENERATED_FIR_TAGS: getter, propertyDeclaration, propertyWithExtensionReceiver */
