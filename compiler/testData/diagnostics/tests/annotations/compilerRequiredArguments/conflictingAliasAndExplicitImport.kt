// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91406
// ALLOW_KOTLIN_PACKAGE
// FIR_DUMP

// FILE: NumericClass.kt
package kotlin

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NumericClass(vararg val actualizations: KClass<*>)

// FILE: other.kt
package other

class Int

// FILE: enumEntry.kt
package enumEntry

import kotlin.annotation.AnnotationTarget.PROPERTY as FIELD
import kotlin.annotation.AnnotationTarget.FIELD

@Target(<!OVERLOAD_RESOLUTION_AMBIGUITY!>FIELD<!>)
annotation class Anno

// FILE: getClass.kt
package getClass

import kotlin.Long as <!CONFLICTING_IMPORT!>Int<!>
import other.<!CONFLICTING_IMPORT!>Int<!>

@NumericClass(Int::class)
class C

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, outProjection, primaryConstructor,
propertyDeclaration, starProjection, vararg */
