// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidAliasedRepeatedAnnotationsOnExpressionsInMultiplatform

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class A

typealias B = A

fun main() {
    val x = @A <!REPEATED_ANNOTATION!>@B<!> 1
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
typeAliasDeclaration */
