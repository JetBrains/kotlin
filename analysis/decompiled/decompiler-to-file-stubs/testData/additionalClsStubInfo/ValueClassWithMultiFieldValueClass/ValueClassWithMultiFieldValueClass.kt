/* Java interop */
// KNM_K2_IGNORE
// FIR_IDENTICAL
// LANGUAGE: +ValueClasses
package test

@JvmInline
value class Foo<T>(val a: T, val b: T)

@JvmInline
value class ValueClassWithMultiFieldValueClass(val foo: Foo<Int>)
