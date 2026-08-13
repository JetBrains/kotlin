// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -JvmIndyAllowLambdasWithAnnotations
// WITH_STDLIB
// ISSUE: KT-87659

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RuntimeAnn

// With the feature disabled, annotated lambdas are compiled to a class, so nothing is reported.
val lambda = @RuntimeAnn {}
val anonymousFunction = @RuntimeAnn fun () {}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousFunction, lambdaLiteral, propertyDeclaration */
