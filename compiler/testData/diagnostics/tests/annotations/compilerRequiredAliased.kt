// RUN_PIPELINE_TILL: FRONTEND

// FILE: q/target.kt
package q

import kotlin.annotation.Target as MyTarget
import p.method

@MyTarget(AnnotationTarget.FIELD)
annotation class MyAnno

@kotlin.annotation.Target(AnnotationTarget.FIELD)
annotation class Anno

class C {
    @MyAnno
    val myField = <!DEPRECATION_ERROR!>method<!>()

    // for reference:
    @Anno
    val field = <!DEPRECATION_ERROR!>method<!>()
}

// FILE: p/deprecation.kt
package p

import kotlin.Deprecated as MyDeprecated

@MyDeprecated("", level = DeprecationLevel.ERROR)
fun method() = 42

fun localClass() {
    @MyDeprecated("", level = DeprecationLevel.ERROR)
    class Local

    <!DEPRECATION_ERROR!>Local<!>()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
