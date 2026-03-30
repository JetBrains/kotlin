// RUN_PIPELINE_TILL: BACKEND

inline fun <R> onlyLocal(crossinline p: () -> R) {
    inlineAll(p)
}

inline fun <R> inlineAll(p: () -> R) {
    p()
}

/* GENERATED_FIR_TAGS: crossinline, functionDeclaration, functionalType, inline, nullableType, typeParameter */
