// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81676
// FILE: unresolved.kt
package a

val list: List<<!UNRESOLVED_REFERENCE!>_<!>>? = null

// FILE: resolved.kt
package b

interface `_`
val list: List<_>? = null

/* GENERATED_FIR_TAGS: nullableType, propertyDeclaration */
