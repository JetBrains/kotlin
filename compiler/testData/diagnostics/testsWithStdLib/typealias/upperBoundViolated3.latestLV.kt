// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ProperlyCheckUpperBoundsViolationsWhenCreatingFreshVariables
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
// TODO(KT-92053): currently, resolves to a.SamAlias
val sa = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>SamAlias<Any> { }<!>

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
