// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

inline fun <R> inlineFunWithAnnotation(crossinline p: () -> R) {
    inlineFun {
        p()
    }
}

inline fun <R> inlineFun2(p: () -> R) {
    inlineFun {
        p()
    }
}

inline fun <R> inlineFun(p: () -> R) {
    p()
}

/* GENERATED_FIR_TAGS: crossinline, functionDeclaration, functionalType, inline, lambdaLiteral, nullableType,
typeParameter */
