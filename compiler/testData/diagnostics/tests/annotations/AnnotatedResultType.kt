// RUN_PIPELINE_TILL: CODEGEN
// Result type can be annotated
@Target(AnnotationTarget.TYPE)
annotation class My(val x: Int)

fun foo(): @My(42) Int = 24

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration */
