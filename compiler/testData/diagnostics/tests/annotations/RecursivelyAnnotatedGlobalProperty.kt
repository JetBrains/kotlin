// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// Properties can be recursively annotated
annotation class ann(val x: Int)
@ann(x) const val x: Int = 1

/* GENERATED_FIR_TAGS: annotationDeclaration, const, integerLiteral, primaryConstructor, propertyDeclaration */
