// RUN_PIPELINE_TILL: BACKEND
public inline fun test(predicate: (Char) -> Boolean) {
    !predicate('c')
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline */
