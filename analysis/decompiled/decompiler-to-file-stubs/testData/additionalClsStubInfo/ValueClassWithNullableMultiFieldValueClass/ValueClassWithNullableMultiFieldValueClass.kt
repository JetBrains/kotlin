/* Java interop */
// KNM_K2_IGNORE
// FIR_IDENTICAL
// LANGUAGE: +ValueClasses
package test

@JvmInline
value class ValueClassWithNullableMultiFieldValueClass(val foo: Foo<Int>?)

@JvmInline
value class Foo<T>(val a: T, val b: T)