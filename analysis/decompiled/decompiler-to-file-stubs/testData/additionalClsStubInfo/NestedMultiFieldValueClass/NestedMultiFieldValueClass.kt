/* Java interop */
// KNM_K2_IGNORE
// FIR_IDENTICAL
// LANGUAGE: +ValueClasses
package test

@JvmInline
value class NestedMultiFieldValueClass<T>(val a: T, val b: T)