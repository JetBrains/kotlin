package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny

internal class OverriddenFunctionDescriptor(val descriptor: FunctionDescriptor, overriddenDescriptor: FunctionDescriptor) {
    val overriddenDescriptor = overriddenDescriptor.original

    val needBridge: Boolean
        get() {
            if (descriptor.modality == Modality.ABSTRACT) return false
            return descriptor.needBridgeTo(overriddenDescriptor)
                    || descriptor.target.needBridgeTo(overriddenDescriptor)
        }

    fun trivialOverride() = descriptor.original == overriddenDescriptor

    override fun toString(): String {
        return "(descriptor=$descriptor, overriddenDescriptor=$overriddenDescriptor)"
    }
}

internal class ClassVtablesBuilder(val classDescriptor: ClassDescriptor, val context: Context) {
    val vtableEntries: List<OverriddenFunctionDescriptor> by lazy {

        assert(!classDescriptor.isInterface)

        val superVtableEntries = if (KotlinBuiltIns.isSpecialClassWithNoSupertypes(classDescriptor)) {
            emptyList()
        } else {
            context.getVtableBuilder(classDescriptor.getSuperClassOrAny()).vtableEntries
        }

        val methods = classDescriptor.contributedMethods
        val newVtableSlots = mutableListOf<OverriddenFunctionDescriptor>()

        val inheritedVtableSlots = superVtableEntries.map { superMethod ->
            val overridingMethod = methods.singleOrNull { OverridingUtil.overrides(it, superMethod.descriptor) }
            if (overridingMethod == null) {
                superMethod
            } else {
                if (!superMethod.trivialOverride())
                    newVtableSlots.add(OverriddenFunctionDescriptor(overridingMethod, superMethod.descriptor))
                // Overriding method is defined in the current class and is not inherited - take it as is.
                newVtableSlots.add(OverriddenFunctionDescriptor(overridingMethod, overridingMethod))
                OverriddenFunctionDescriptor(overridingMethod, superMethod.overriddenDescriptor)
            }
        }

        methods.filterNot { method -> inheritedVtableSlots.any { it.descriptor == method } } // Find newly defined methods.
                // Select target because method might be taken from default implementation of interface.
                .mapTo(newVtableSlots) { OverriddenFunctionDescriptor(it, it.target) }

        val list = inheritedVtableSlots + newVtableSlots.filter { it.descriptor.isOverridable }.sortedBy {
            it.overriddenDescriptor.functionName.localHash.value
        }
        list
    }

    fun vtableIndex(function: FunctionDescriptor): Int {
        val target = function.target
        val index = vtableEntries.indexOfFirst { it.overriddenDescriptor == target }
        if (index < 0) throw Error(function.toString() + " not in vtable of " + classDescriptor.toString())
        return index
    }

    private val ClassDescriptor.contributedMethodsWithOverridden: List<OverriddenFunctionDescriptor>
        get() {
            return contributedMethods.flatMap { method ->
                method.allOverriddenDescriptors.map { OverriddenFunctionDescriptor(method, it) }
            }.distinctBy {
                Triple(it.overriddenDescriptor.functionName, it.descriptor, it.needBridge)
            }.sortedBy {
                it.overriddenDescriptor.functionName.localHash.value
            }
        }

    val methodTableEntries: List<OverriddenFunctionDescriptor> by lazy {
        assert(!classDescriptor.isAbstract())

        classDescriptor.contributedMethodsWithOverridden.filter {
            // We check that either method is open, or one of declarations it overrides
            // is open.
            it.overriddenDescriptor.isOverridable || DescriptorUtils.getAllOverriddenDeclarations(it.overriddenDescriptor).any { it.isOverridable }
        }
        // TODO: probably method table should contain all accessible methods to improve binary compatibility
    }

}