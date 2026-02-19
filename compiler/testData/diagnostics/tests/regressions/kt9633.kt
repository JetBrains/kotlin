// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// KT-9633: SOE occurred before
interface A<<!FINITE_BOUNDS_VIOLATION!>T : A<in T><!>>

/* GENERATED_FIR_TAGS: inProjection, interfaceDeclaration, typeConstraint, typeParameter */
