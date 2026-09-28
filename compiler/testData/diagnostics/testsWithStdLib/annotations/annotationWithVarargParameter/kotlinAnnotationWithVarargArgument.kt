// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: CollectionLiteralsBasedAnnotationResolution
annotation class B(vararg val args: String)

@B(*<!ARGUMENT_TYPE_MISMATCH!><!TYPE_INTERSECTION_AS_REIFIED_ERROR!>arrayOf<!>(1, "b")<!>)
fun test() {
}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, integerLiteral, outProjection,
primaryConstructor, propertyDeclaration, stringLiteral, vararg */
