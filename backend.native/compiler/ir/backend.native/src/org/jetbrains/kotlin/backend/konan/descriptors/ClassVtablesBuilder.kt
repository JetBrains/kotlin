/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.simpleFunctions

internal class OverriddenFunctionDescriptor(
        val descriptor: SimpleFunctionDescriptor,
        overriddenDescriptor: SimpleFunctionDescriptor
) {
    val overriddenDescriptor = overriddenDescriptor.original

    val needBridge: Boolean
        get() = descriptor.target.needBridgeTo(overriddenDescriptor)

    val bridgeDirections: BridgeDirections
        get() = descriptor.target.bridgeDirectionsTo(overriddenDescriptor)

    val canBeCalledVirtually: Boolean
        get() {
            if (overriddenDescriptor.isObjCClassMethod()) {
                return descriptor.canObjCClassMethodBeCalledVirtually(this.overriddenDescriptor)
            }

            return overriddenDescriptor.isOverridable
        }

    val inheritsBridge: Boolean
        get() = !descriptor.isReal
                && descriptor.target.overrides(overriddenDescriptor)
                && descriptor.bridgeDirectionsTo(overriddenDescriptor).allNotNeeded()

    fun getImplementation(context: Context): SimpleFunctionDescriptor? {
        val target = descriptor.target
        val implementation = if (!needBridge)
            target
        else {
            val bridgeOwner = if (inheritsBridge) {
                target // Bridge is inherited from superclass.
            } else {
                descriptor
            }
            context.specialDeclarationsFactory.getBridgeDescriptor(OverriddenFunctionDescriptor(bridgeOwner, overriddenDescriptor))
        }
        return if (implementation.modality == Modality.ABSTRACT) null else implementation
    }

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
    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    val vtableEntries: List<OverriddenFunctionDescriptor> by lazy {

        assert(!classDescriptor.isInterface)

        DEBUG_OUTPUT(0) {
            println()
            println("BUILDING vTable for ${classDescriptor.descriptor}")
        }

        val superVtableEntries = if (classDescriptor.isSpecialClassWithNoSupertypes()) {
            emptyList()
        } else {
            val superClass = classDescriptor.getSuperClassNotAny() ?: context.ir.symbols.any.owner
            context.getVtableBuilder(superClass).vtableEntries
        }

        val methods = classDescriptor.sortedOverridableOrOverridingMethods
        val newVtableSlots = mutableListOf<OverriddenFunctionDescriptor>()

        DEBUG_OUTPUT(0) {
            println()
            println("SUPER vTable:")
            superVtableEntries.forEach { println("    ${it.overriddenDescriptor.descriptor} -> ${it.descriptor.descriptor}") }

            println()
            println("METHODS:")
            methods.forEach { println("    ${it.descriptor}") }

            println()
            println("BUILDING INHERITED vTable")
        }

        val inheritedVtableSlots = superVtableEntries.map { superMethod ->
            val overridingMethod = methods.singleOrNull { it.overrides(superMethod.descriptor) }
            if (overridingMethod == null) {

                DEBUG_OUTPUT(0) { println("Taking super ${superMethod.overriddenDescriptor.descriptor} -> ${superMethod.descriptor.descriptor}") }

                superMethod
            } else {
                newVtableSlots.add(OverriddenFunctionDescriptor(overridingMethod, superMethod.descriptor))

                DEBUG_OUTPUT(0) { println("Taking overridden ${superMethod.overriddenDescriptor.descriptor} -> ${overridingMethod.descriptor}") }

                OverriddenFunctionDescriptor(overridingMethod, superMethod.overriddenDescriptor)
            }
        }

        // Add all possible (descriptor, overriddenDescriptor) edges for now, redundant will be removed later.
        methods.mapTo(newVtableSlots) { OverriddenFunctionDescriptor(it, it) }

        val inheritedVtableSlotsSet = inheritedVtableSlots.map { it.descriptor to it.bridgeDirections }.toSet()

        val filteredNewVtableSlots = newVtableSlots
                .filterNot { inheritedVtableSlotsSet.contains(it.descriptor to it.bridgeDirections) }
                .distinctBy { it.descriptor to it.bridgeDirections }
                .filter { it.descriptor.isOverridable }

        DEBUG_OUTPUT(0) {
            println()
            println("INHERITED vTable slots:")
            inheritedVtableSlots.forEach { println("    ${it.overriddenDescriptor.descriptor} -> ${it.descriptor.descriptor}") }

            println()
            println("MY OWN vTable slots:")
            filteredNewVtableSlots.forEach { println("    ${it.overriddenDescriptor.descriptor} -> ${it.descriptor.descriptor}") }
        }

        inheritedVtableSlots + filteredNewVtableSlots.sortedBy { it.overriddenDescriptor.functionName.localHash.value }
    }

    fun vtableIndex(function: SimpleFunctionDescriptor): Int {
        val bridgeDirections = function.target.bridgeDirectionsTo(function.original)
        val index = vtableEntries.indexOfFirst { it.descriptor == function.original && it.bridgeDirections == bridgeDirections }
        if (index < 0) throw Error(function.toString() + " not in vtable of " + classDescriptor.toString())
        return index
    }

    val methodTableEntries: List<OverriddenFunctionDescriptor> by lazy {
        classDescriptor.sortedOverridableOrOverridingMethods
                .flatMap { method -> method.allOverriddenDescriptors.map { OverriddenFunctionDescriptor(method, it) } }
                .filter { it.canBeCalledVirtually }
                .distinctBy { Triple(it.overriddenDescriptor.functionName, it.descriptor, it.needBridge) }
                .sortedBy { it.overriddenDescriptor.functionName.localHash.value }
        // TODO: probably method table should contain all accessible methods to improve binary compatibility
    }

}

private val IrClass.sortedOverridableOrOverridingMethods: List<SimpleFunctionDescriptor>
    get() =
        this.simpleFunctions()
                .filter { it.isOverridable || it.overriddenSymbols.isNotEmpty() }
                // TODO: extract method .isBridge()
                .filterNot { it.name.asString().contains("<bridge-") }
                .sortedBy { it.functionName.localHash.value }
