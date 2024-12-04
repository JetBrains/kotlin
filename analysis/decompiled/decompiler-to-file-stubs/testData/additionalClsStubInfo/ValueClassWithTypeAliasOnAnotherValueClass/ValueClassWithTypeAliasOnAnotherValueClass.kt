/* Java interop */
// KNM_K2_IGNORE
// FIR_IDENTICAL
package pack

@JvmInline
value class AnotherValueClass(val s: String)

typealias MyTypeAlias = AnotherValueClass

@JvmInline
value class ValueClassWithTypeAliasOnAnotherValueClass(val value: MyTypeAlias)