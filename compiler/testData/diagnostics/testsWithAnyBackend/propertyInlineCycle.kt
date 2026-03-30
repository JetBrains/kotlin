// RUN_PIPELINE_TILL: BACKEND
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

inline val String.foo: String
    get() = <!INLINE_CALL_CYCLE!>foo<!>

/* GENERATED_FIR_TAGS: getter, propertyDeclaration, propertyWithExtensionReceiver */
