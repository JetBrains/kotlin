// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// Class constructor parameter type CAN be recursively annotated
@Target(AnnotationTarget.TYPE)
annotation class RecursivelyAnnotated(val x: @RecursivelyAnnotated(1) Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
