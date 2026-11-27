// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ReportUpperBoundViolatedWithSamTypealiasedConstructors
// LANGUAGE_FEATURE_TOGGLED: ProperlyUnwrapTypealiasedSamConstructorsWhenBuildingFreshVariables
// FIR_DUMP
// FILE: A.kt
package a

class Base<K : List<CharSequence>>
typealias Alias<T> = Base<List<T>>

fun interface Sam<K : List<CharSequence>> {
    fun bar()
}
typealias SamAlias<T> = Sam<List<T>>

// FILE: B.kt
package b

import a.Alias
import a.SamAlias

class Box<B>
typealias Alias<K> = Box<K>

fun interface SamBox<B> {
    fun bar()
}
typealias SamAlias<K> = SamBox<K>

val a = Alias<Any>()
val sa = SamAlias<Any> { }

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
