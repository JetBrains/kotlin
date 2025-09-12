// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-80871

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation(vararg val dummy: @MyAnnotation String)

/* GENERATED_FIR_TAGS: annotationDeclaration, outProjection, primaryConstructor, propertyDeclaration, vararg */
