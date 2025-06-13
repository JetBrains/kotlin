// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

fun test(collection: MutableCollection<Boolean>) {
    collection.removeIf { it }
}

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, lambdaLiteral, samConversion */
