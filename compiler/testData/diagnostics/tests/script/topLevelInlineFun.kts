// RUN_PIPELINE_TILL: BACKEND

inline fun foo(f: () -> Unit) {
    f()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, localProperty, propertyDeclaration */
