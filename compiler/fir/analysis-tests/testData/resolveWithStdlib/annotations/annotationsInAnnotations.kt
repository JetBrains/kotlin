// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +PropertyParamAnnotationDefaultTargetMode

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class ParamProperty

annotation class Annot(
    <!ANNOTATION_IN_ANNOTATION_PARAMETER_REQUIRES_TARGET("targets 'param', 'property'")!>@ParamProperty<!>
    val s: String
)

/* GENERATED_FIR_TAGS: annotationDeclaration, primaryConstructor, propertyDeclaration */
