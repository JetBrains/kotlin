// RUN_PIPELINE_TILL: BACKEND
// Class CAN be recursively annotated
@RecursivelyAnnotated(1)
annotation class RecursivelyAnnotated(val x: Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
