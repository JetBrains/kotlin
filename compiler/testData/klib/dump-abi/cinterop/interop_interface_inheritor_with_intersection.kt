// MODULE: interop_interface_inheritor_with_intersection_library

@file:Suppress("RedundantModalityModifier")

package interop_interface_inheritor_with_intersection.test

import interop_interface_inheritor_with_intersection.NSObject
import interop_interface_inheritor_with_intersection.AProtocol
import interop_interface_inheritor_with_intersection.BProtocol

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Derived : NSObject(), AProtocol, BProtocol {
    override fun overriddenFunction() = Unit
    override fun overriddenProperty(): Int = 42
    override fun setOverriddenProperty(overriddenProperty: Int) = Unit
}
