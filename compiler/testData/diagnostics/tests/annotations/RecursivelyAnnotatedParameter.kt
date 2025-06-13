// RUN_PIPELINE_TILL: BACKEND
// Class constructor parameter CAN be recursively annotated
annotation class RecursivelyAnnotated(@RecursivelyAnnotated(1) val x: Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
