// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-13398
// WITH_STDLIB

// KT-13398: Safe call on a generic type with nullable upper bound doesn't make it possible to call javaClass extension

fun <T> describe(t: T) {
    println(t?.javaClass)
}

fun <T : Any?> describe2(t: T) {
    println(t?.javaClass)
}

/* GENERATED_FIR_TAGS: dnnType, functionDeclaration, nullableType, safeCall, typeConstraint, typeParameter */
