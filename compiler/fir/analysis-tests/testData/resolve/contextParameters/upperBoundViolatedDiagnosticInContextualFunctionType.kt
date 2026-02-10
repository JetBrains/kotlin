// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-83354
class SortedList<T : Comparable<T>>

fun <T, U> foo(action: context(SortedList<T>) <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>U<!>.() -> Unit) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, nullableType, typeConstraint,
typeParameter, typeWithContext, typeWithExtension */
