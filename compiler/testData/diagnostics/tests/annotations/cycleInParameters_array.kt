// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-52742

annotation class AnnotationWithArray(
    val array: Array<AnnotationWithArray>
)

annotation class AnnotationWithVararg(
    vararg val args: AnnotationWithVararg
)

/* GENERATED_FIR_TAGS: annotationDeclaration, outProjection, primaryConstructor, propertyDeclaration, vararg */
