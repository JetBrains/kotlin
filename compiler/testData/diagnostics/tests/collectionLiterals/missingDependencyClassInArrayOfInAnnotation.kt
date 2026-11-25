// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CollectionLiterals
// LANGUAGE_FEATURE_TOGGLED: CollectionLiteralsBasedAnnotationResolution
// WITH_STDLIB
// ISSUE: KT-89259

// MODULE: m1
// FILE: f1.kt
package p1

open class Missing

// MODULE: m2(m1)
// FILE: f2.kt
package p2

import kotlin.reflect.KClass

open class ChildOfMissing : p1.Missing()

annotation class WithMissingInParams(val param: Array<KClass<out p1.Missing>>)
annotation class WithChildOfMissingInParams(val param: Array<KClass<out ChildOfMissing>>)

// MODULE: m3(m2)
// FILE: f3.kt
package p3

import kotlin.reflect.KClass

@p2.WithMissingInParams([])
@p2.WithChildOfMissingInParams([])
class Annotated

@p2.WithMissingInParams(arrayOf())
@p2.WithChildOfMissingInParams(arrayOf())
class Annotated2

annotation class SuperClassMissingInConstructor(val param: Array<KClass<p2.ChildOfMissing>> = [])
annotation class SuperClassMissingInConstructorVararg(vararg val param: KClass<p2.ChildOfMissing> = [])

fun forReference() {
    p2.WithChildOfMissingInParams(arrayOf())
    p2.<!MISSING_DEPENDENCY_CLASS!>WithMissingInParams<!>(<!MISSING_DEPENDENCY_CLASS!>arrayOf<!>())
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, outProjection, primaryConstructor, propertyDeclaration */
