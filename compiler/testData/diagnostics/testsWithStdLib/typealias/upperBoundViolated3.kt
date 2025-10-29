// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
package a

class Base<K : List<CharSequence>>
typealias Alias<T> = Base<List<T>>

// FILE: B.kt
package b

import a.Alias

class Box<B>
typealias Alias<K> = Box<K>

val a = Alias<<!UPPER_BOUND_VIOLATED_WARNING!>Any<!>>()

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
