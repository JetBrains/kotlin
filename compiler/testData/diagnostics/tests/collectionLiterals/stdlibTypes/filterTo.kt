// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

fun foo() {
    val x = [1, 2, 3].filterTo(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>) { it % 2 == 0 }
    val y: MutableSet<Int> = [1, 2, 3].filterTo([]) { it % 2 == 0 }
    val z: Set<Int> = [1, 2, 3].filterTo(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>) { it % 2 == 0 }
}

/* GENERATED_FIR_TAGS: collectionLiteral, equalityExpression, functionDeclaration, inProjection, integerLiteral,
lambdaLiteral, localProperty, multiplicativeExpression, propertyDeclaration */
