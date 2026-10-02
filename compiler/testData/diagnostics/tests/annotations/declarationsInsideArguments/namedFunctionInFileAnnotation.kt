// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041
@file:Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)

package testPack

@Target(AnnotationTarget.FILE)
annotation class Anno(val i: Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, functionDeclaration, integerLiteral,
localFunction, primaryConstructor, propertyDeclaration */
