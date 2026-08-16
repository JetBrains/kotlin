// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +JvmIndyAllowLambdasWithAnnotations
// WITH_STDLIB
// ISSUE: KT-87659

import kotlin.jvm.JvmSerializableLambda

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RuntimeAnn

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class BinaryAnn

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnn

inline fun inlineArg(x: () -> Unit) = x()
inline fun crossinlineArg(crossinline x: () -> Unit) = { x() }
<!NOTHING_TO_INLINE!>inline<!> fun noinlineArg(noinline x: () -> Unit) = x

// Annotations are dropped by the 'invokedynamic' generation scheme.
val lambda = <!RUNTIME_ANNOTATION_ON_LAMBDA_IS_NOT_RETAINED!>@RuntimeAnn<!> {}
val anonymousFunction = <!RUNTIME_ANNOTATION_ON_LAMBDA_IS_NOT_RETAINED!>@RuntimeAnn<!> fun () {}
val extensionAnonymousFunction = <!RUNTIME_ANNOTATION_ON_LAMBDA_IS_NOT_RETAINED!>@RuntimeAnn<!> fun Any.() {}

// Only runtime retention matters.
val binary = @BinaryAnn {}
val source = @SourceAnn {}

// '@JvmSerializableLambda' forces the class generation scheme.
val serializable = @JvmSerializableLambda @RuntimeAnn {}

// Suspend lambdas are always compiled to a class.
val suspendLambda: suspend () -> Unit = @RuntimeAnn {}

fun test() {
    // Inlined lambdas are covered by NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION.
    inlineArg <!NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION!>@RuntimeAnn<!> {}
    crossinlineArg <!NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION!>@RuntimeAnn<!> {}
    noinlineArg <!RUNTIME_ANNOTATION_ON_LAMBDA_IS_NOT_RETAINED!>@RuntimeAnn<!> {}
}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousFunction, crossinline, functionDeclaration, functionalType,
inline, lambdaLiteral, noinline, propertyDeclaration, suspend */
