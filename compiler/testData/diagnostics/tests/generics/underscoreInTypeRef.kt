// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81676
// FILE: unresolved.kt
package a

val list: List<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>? = null

val list2: List<<!UNRESOLVED_REFERENCE!>`_`<!>>? = null
val list3: List<a.<!UNRESOLVED_REFERENCE!>_<!>>? = null

fun foo(l: List<<!PLACEHOLDER_PROJECTION_IN_TYPEREF!>_<!>>) {}

// FILE: resolved.kt
package b

interface `_`
val list: List<_>? = null

/* GENERATED_FIR_TAGS: nullableType, propertyDeclaration */
