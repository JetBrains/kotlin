// LL_FIR_DIVERGENCE
//   the placement of @Anno (field / property) depends on the resolve phase of Anno
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91406

// FILE: p1.kt
package p

import kotlin.annotation.AnnotationTarget.PROPERTY as FIELD
import kotlin.annotation.AnnotationTarget.*

@Target(<!AMBIGUOUS_ANNOTATION_ARGUMENT!>FIELD<!>)
annotation class Anno

class Simple {
    @Anno
    val prop: Int = 42
}

// FILE: p2.kt
package p

import kotlin.DeprecationLevel.*
import kotlin.DeprecationLevel.WARNING as ERROR

@Deprecated("", level = <!AMBIGUOUS_ANNOTATION_ARGUMENT!>ERROR<!>)
fun foo() {}

fun main() {
    <!DEPRECATION_ERROR!>foo<!>()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
stringLiteral */
