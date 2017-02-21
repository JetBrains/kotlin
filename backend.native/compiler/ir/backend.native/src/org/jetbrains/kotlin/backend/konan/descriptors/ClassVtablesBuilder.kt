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
            return descriptor.kind == CallableMemberDescriptor.Kind.DELEGATION
                    || descriptor.target.needBridgeTo(overriddenDescriptor)
        }

    val bridgeDirections: BridgeDirections
        get() = descriptor.target.bridgeDirectionsTo(overriddenDescriptor)

    val canBeCalledVirtually: Boolean
            // We check that either method is open, or one of declarations it overrides is open.
        get() = overriddenDescriptor.isOverridable
                || DescriptorUtils.getAllOverriddenDeclarations(overriddenDescriptor).any { it.isOverridable }


    override fun toString(): String {
        return "(descriptor=$descriptor, overriddenDescriptor=$overriddenDescriptor)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverriddenFunctionDescriptor) return false

        if (descriptor != other.descriptor) return false
        if (overriddenDescriptor != other.overriddenDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = descriptor.hashCode()
        result = 31 * result + overriddenDescriptor.hashCode()
        return result
    }
}

internal class ClassVtablesBuilder(val classDescriptor: ClassDescriptor, val context: Context) {
    val vtableEntries: List<OverriddenFunctionDescriptor> /*by lazy*/ get() {

        println("VTABLE_ENTRIES class: $classDescriptor")

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
                newVtableSlots.add(OverriddenFunctionDescriptor(overridingMethod, superMethod.descriptor))
                OverriddenFunctionDescriptor(overridingMethod, superMethod.overriddenDescriptor)
            }
        }

        println("VTABLE_ENTRIES inherited vtable:")
        inheritedVtableSlots.forEach {
            println("   VTABLE_ENTRIES descriptor: ${it.descriptor}")
            println("   VTABLE_ENTRIES overriddenDescriptor: ${it.overriddenDescriptor}")
            println("   VTABLE_ENTRIES needBridge: ${it.needBridge}")
            if (it.needBridge)
                println("   VTABLE_ENTRIES bridgeDirections: ${it.bridgeDirections.toString()}")
        }
        println()


        // Add all possible (descriptor, overriddenDescriptor) edges for now, redundant will be removed later.
        methods.mapTo(newVtableSlots) { OverriddenFunctionDescriptor(it, it) }

        val inheritedVtableSlotsSet = inheritedVtableSlots.map { it.descriptor to it.bridgeDirections }.toSet()

        val filteredNewVtableSlots = newVtableSlots
                .filterNot { inheritedVtableSlotsSet.contains(it.descriptor to it.bridgeDirections) }
                .distinctBy { it.descriptor to it.bridgeDirections }
                .filter { it.descriptor.isOverridable }
        println("VTABLE_ENTRIES new vtable:")
        filteredNewVtableSlots.forEach {
            println("   VTABLE_ENTRIES descriptor: ${it.descriptor}")
            println("   VTABLE_ENTRIES overriddenDescriptor: ${it.overriddenDescriptor}")
            println("   VTABLE_ENTRIES needBridge: ${it.needBridge}")
            if (it.needBridge)
                println("   VTABLE_ENTRIES bridgeDirections: ${it.bridgeDirections.toString()}")
        }
        println()


        val list = inheritedVtableSlots + filteredNewVtableSlots.sortedBy { it.overriddenDescriptor.functionName.localHash.value }

        println("VTABLE_ENTRIES vtable:")
        list.forEach {
            println("   VTABLE_ENTRIES descriptor: ${it.descriptor}")
            println("   VTABLE_ENTRIES overriddenDescriptor: ${it.overriddenDescriptor}")
            println("   VTABLE_ENTRIES needBridge: ${it.needBridge}")
            if (it.needBridge)
                println("   VTABLE_ENTRIES bridgeDirections: ${it.bridgeDirections.toString()}")
        }
        println()
        println()


        return list
    }

    fun vtableIndex(function: FunctionDescriptor): Int {
        val target = function.target
        val bridgeDirections = target.bridgeDirectionsTo(function.original)
        println("VTABLE_INDEX function: $function")
        println("VTABLE_INDEX original: ${function.original}")
        println("VTABLE_INDEX target: ${target}")
        println("VTABLE_INDEX bridgeDirections: ${target.bridgeDirectionsTo(function.original).toString()}")
        val index = vtableEntries.indexOfFirst { it.descriptor == function.original && it.bridgeDirections == bridgeDirections }
        if (index < 0) throw Error(function.toString() + " not in vtable of " + classDescriptor.toString())
        return index.apply { println("VTABLE_INDEX index: $this"); println() }
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

    val methodTableEntries: List<OverriddenFunctionDescriptor> /*by lazy*/ get() {
        assert(!classDescriptor.isAbstract())

        println("METHOD_TABLE_ENTRIES: class = $this")

        val contributedMethodsWithOverridden = classDescriptor.contributedMethodsWithOverridden
        println("METHOD_TABLE_ENTRIES: contributed methods")
        contributedMethodsWithOverridden.forEach {
            println("   METHOD_TABLE_ENTRIES: descriptor = ${it.descriptor}")
            println("   METHOD_TABLE_ENTRIES: overriddenDescriptor = ${it.overriddenDescriptor}")
            println("   METHOD_TABLE_ENTRIES: overriddenDescriptor.isOverridable = ${it.overriddenDescriptor.isOverridable}")
        }
        val result = contributedMethodsWithOverridden.filter { it.canBeCalledVirtually }

        println("METHOD_TABLE_ENTRIES: result")
        result.forEach {
            println("   METHOD_TABLE_ENTRIES: descriptor = ${it.descriptor}")
            println("   METHOD_TABLE_ENTRIES: overriddenDescriptor = ${it.overriddenDescriptor}")
        }
        return result
        // TODO: probably method table should contain all accessible methods to improve binary compatibility
    }

}