// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

inline fun foo(f: () -> Unit) {
    f()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, localProperty, propertyDeclaration */
