// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-91406
// FIR_DUMP

// FILE: p1.kt
package p

import kotlin.annotation.AnnotationTarget.PROPERTY as FIELD
import kotlin.annotation.AnnotationTarget.*

@Target(FIELD)
annotation class Anno

class Simple {
    @Anno
    val prop: Int = 42
}

// FILE: p2.kt
package p

import kotlin.DeprecationLevel.*
import kotlin.DeprecationLevel.WARNING as ERROR

@Deprecated("", level = ERROR)
fun foo() {}

fun main() {
    <!DEPRECATION!>foo<!>()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
stringLiteral */
