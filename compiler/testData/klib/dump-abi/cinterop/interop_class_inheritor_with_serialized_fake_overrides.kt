// MODULE: interop_class_inheritor_with_serialized_fake_overrides
// ISSUE: KT-72812
// KLIB_ABI_LEVEL: ABI_LEVEL_2_3

@file:Suppress("RedundantModalityModifier")

package interop_class_inheritor_with_serialized_fake_overrides.test

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Derived : interop_class_inheritor_with_serialized_fake_overrides.Base() {
    override fun overriddenFunction() = Unit
    //override fun nonOverriddenFunction() = Unit

    override fun overriddenProperty(): Int = 42
    //override fun nonOverriddenProperty(): Int = 42

    override fun setOverriddenProperty(overriddenProperty: Int) = Unit
    //override fun setNonOverriddenProperty(nonOverriddenProperty: Int) = Unit
}
