// FIR_IDENTICAL
// LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// ISSUE: KT-19455

package test

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

open class TypeToken<T>

object Test : TypeToken<@TypeAnn String>() // (1)

val test = object : TypeToken<@TypeAnn String>() {} // (2)
