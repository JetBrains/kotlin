// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE

// FILE: OldMustUse.kt

package kotlin

@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS)
@SinceKotlin("2.2")
public annotation class MustUseReturnValue

// FILE: Test.kt

<!IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED!>@file:MustUseReturnValue<!>

import kotlin.MustUseReturnValue

fun foo(): String = ""

<!IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED!>@MustUseReturnValue<!>
class Test {
    fun method(): Double = 0.0
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, classDeclaration, functionDeclaration,
stringLiteral */
