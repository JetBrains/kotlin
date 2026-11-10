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
class MyInt

// FILE: explicitStarImport.kt
package explicitStarImport

import other.*

@NumericClass(Int::class)
class C

// FILE: samePackage.kt
package samePackage

import kotlin.Int

class Int

@NumericClass(Int::class)
class C

// FILE: aliasedImportAndStarImport.kt
package aliasedImportAndStarImport

import other.*
import kotlin.Int as MyInt

@NumericClass(<!AMBIGUOUS_ANNOTATION_ARGUMENT!>MyInt<!>::class)
class C

// FILE: aliasedImportAndSamePackage.kt
package aliasedImportAndSamePackage

import kotlin.Int as MyInt

class MyInt

@NumericClass(<!AMBIGUOUS_ANNOTATION_ARGUMENT!>MyInt<!>::class)
class C

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, outProjection, primaryConstructor,
propertyDeclaration, starProjection, vararg */
