// FIR_IDENTICAL
// KT-9633: SOE occurred before
interface A<<!FINITE_BOUNDS_VIOLATION!>T : A<in T><!>>