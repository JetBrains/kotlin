// RUN_PIPELINE_TILL: BACKEND

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
    val myField = method()

    // for reference:
    @Anno
    val field = method()
}

// FILE: p/deprecation.kt
package p

import kotlin.Deprecated as MyDeprecated

@MyDeprecated("", level = DeprecationLevel.ERROR)
fun method() = 42

fun localClass() {
    @MyDeprecated("", level = DeprecationLevel.ERROR)
    class Local

    Local()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
