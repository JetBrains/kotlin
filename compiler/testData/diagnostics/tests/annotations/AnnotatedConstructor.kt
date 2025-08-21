// RUN_PIPELINE_TILL: BACKEND
annotation class ann
class Annotated(@ann val x: Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, primaryConstructor, propertyDeclaration */
