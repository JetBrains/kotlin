/* Java interop */
// KNM_K2_IGNORE
// FIR_IDENTICAL
// LANGUAGE: +ValueClasses
package pack

typealias MyAlias<A> = List<A>

@JvmInline
value class MultiFieldValueClassWithTypeAlias<T>(val alias: MyAlias<T>, val b: String)