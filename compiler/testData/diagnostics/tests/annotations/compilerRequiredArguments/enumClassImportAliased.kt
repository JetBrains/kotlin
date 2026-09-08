// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87025

// FILE: q/target.kt
package q

import kotlin.annotation.AnnotationTarget as MyAnnotationTarget

@Target(MyAnnotationTarget.FIELD)
annotation class ViaAlias

@Target(<!UNRESOLVED_REFERENCE!>AnnotationTarget<!>.FIELD)
annotation class DoesNotResolve

@Target(kotlin.annotation.AnnotationTarget.FIELD)
annotation class StillResolves

class C {
    @ViaAlias
    val a = 1
}

// FILE: p/deprecation.kt
package p

import kotlin.DeprecationLevel as MyDeprecationLevel

@Deprecated("", level = MyDeprecationLevel.ERROR)
fun aliasedLevel() {}

fun use() {
    <!DEPRECATION_ERROR!>aliasedLevel<!>()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
stringLiteral */
