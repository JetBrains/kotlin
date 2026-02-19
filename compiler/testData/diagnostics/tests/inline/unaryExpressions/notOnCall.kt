// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
public inline fun test(predicate: (Char) -> Boolean) {
    !predicate('c')
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline */
