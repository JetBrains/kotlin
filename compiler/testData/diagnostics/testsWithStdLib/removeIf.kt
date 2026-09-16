// RUN_PIPELINE_TILL: CODEGEN
// FULL_JDK

fun test(collection: MutableCollection<Boolean>) {
    collection.removeIf { it }
}

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, lambdaLiteral, samConversion */
