/* Java interop */
// KNM_K2_IGNORE
// FIR_IDENTICAL
package pack

@JvmInline
value class AnotherValueClass(val s: String)

@JvmInline
value class ValueClassWithAnotherValueClass(val value: AnotherValueClass)