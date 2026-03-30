// RUN_PIPELINE_TILL: BACKEND
@Target(AnnotationTarget.CLASS)
annotation class base

@base data class My(val x: Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, primaryConstructor, propertyDeclaration */
