/* Java interop */
// KNM_K2_IGNORE
// FIR_IDENTICAL
package test

@JvmInline
value class ValueClassWithNestedClass(val value: NestedClass) {
    class NestedClass
}
