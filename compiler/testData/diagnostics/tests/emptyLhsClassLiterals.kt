// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-59165

fun regular() {
    <!UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS!>::class<!> // K1: Error, K2: compiles
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration */
