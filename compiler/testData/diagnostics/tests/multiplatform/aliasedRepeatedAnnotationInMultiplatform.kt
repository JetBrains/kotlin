// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KTLC-409
// LANGUAGE: +ForbidAliasedRepeatedAnnotationsOnExpressionsInMultiplatform

// MODULE: common
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class A

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
expect annotation class B()

fun println() {}

fun foo() {
    @A <!REPEATED_ANNOTATION!>@B<!>
    println()
}

// MODULE: platform()()(common)
actual typealias B = A

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, expect, functionDeclaration, typeAliasDeclaration */
