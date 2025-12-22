// MODULE: interop_class_inheritor_library_2_4
// KLIB_ABI_LEVEL: ABI_LEVEL_2_4

@file:Suppress("RedundantModalityModifier")

package interop_class_inheritor.test

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Derived : interop_class_inheritor_2_4.Base() {
    override fun overriddenFunction() = Unit
    //override fun nonOverriddenFunction() = Unit

    override fun overriddenProperty(): Int = 42
    //override fun nonOverriddenProperty(): Int = 42

    override fun setOverriddenProperty(overriddenProperty: Int) = Unit
    //override fun setNonOverriddenProperty(nonOverriddenProperty: Int) = Unit
}
