/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.backend.konan.lower.bridgeTarget
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.simpleFunctions

internal class OverriddenFunctionInfo(
        val function: IrSimpleFunction,
        val overriddenFunction: IrSimpleFunction
) {
    val needBridge: Boolean
        get() = function.target.needBridgeTo(overriddenFunction)

    val bridgeDirections: BridgeDirections
        get() = function.target.bridgeDirectionsTo(overriddenFunction)

    val canBeCalledVirtually: Boolean
        get() {
            if (overriddenFunction.isObjCClassMethod()) {
                return function.canObjCClassMethodBeCalledVirtually(overriddenFunction)
            }

            return overriddenFunction.isOverridable
        }

    val inheritsBridge: Boolean
        get() = !function.isReal
                && function.target.overrides(overriddenFunction)
                && function.bridgeDirectionsTo(overriddenFunction).allNotNeeded()

    fun getImplementation(context: Context): IrSimpleFunction? {
        val target = function.target
        val implementation = if (!needBridge)
            target
        else {
            val bridgeOwner = if (inheritsBridge) {
                target // Bridge is inherited from superclass.
            } else {
                function
            }
            context.specialDeclarationsFactory.getBridge(OverriddenFunctionInfo(bridgeOwner, overriddenFunction))
        }
        return if (implementation.modality == Modality.ABSTRACT) null else implementation
    }

    override fun toString(): String {
        return "(descriptor=$function, overriddenDescriptor=$overriddenFunction)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverriddenFunctionInfo) return false

        if (function != other.function) return false
        if (overriddenFunction != other.overriddenFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = function.hashCode()
        result = 31 * result + overriddenFunction.hashCode()
        return result
    }
}

internal class ClassVtablesBuilder(val irClass: IrClass, val context: Context) {
    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    val vtableEntries: List<OverriddenFunctionInfo> by lazy {

        assert(!irClass.isInterface)

        DEBUG_OUTPUT(0) {
            println()
            println("BUILDING vTable for ${irClass.descriptor}")
        }

        val superVtableEntries = if (irClass.isSpecialClassWithNoSupertypes()) {
            emptyList()
        } else {
            val superClass = irClass.getSuperClassNotAny() ?: context.ir.symbols.any.owner
            context.getVtableBuilder(superClass).vtableEntries
        }

        val methods = irClass.sortedOverridableOrOverridingMethods
        val newVtableSlots = mutableListOf<OverriddenFunctionInfo>()

        DEBUG_OUTPUT(0) {
            println()
            println("SUPER vTable:")
            superVtableEntries.forEach { println("    ${it.overriddenFunction.descriptor} -> ${it.function.descriptor}") }

            println()
            println("METHODS:")
            methods.forEach { println("    ${it.descriptor}") }

            println()
            println("BUILDING INHERITED vTable")
        }

        val inheritedVtableSlots = superVtableEntries.map { superMethod ->
            val overridingMethod = methods.singleOrNull { it.overrides(superMethod.function) }
            if (overridingMethod == null) {

                DEBUG_OUTPUT(0) { println("Taking super ${superMethod.overriddenFunction.descriptor} -> ${superMethod.function.descriptor}") }

                superMethod
            } else {
                newVtableSlots.add(OverriddenFunctionInfo(overridingMethod, superMethod.function))

                DEBUG_OUTPUT(0) { println("Taking overridden ${superMethod.overriddenFunction.descriptor} -> ${overridingMethod.descriptor}") }

                OverriddenFunctionInfo(overridingMethod, superMethod.overriddenFunction)
            }
        }

        // Add all possible (descriptor, overriddenDescriptor) edges for now, redundant will be removed later.
        methods.mapTo(newVtableSlots) { OverriddenFunctionInfo(it, it) }

        val inheritedVtableSlotsSet = inheritedVtableSlots.map { it.function to it.bridgeDirections }.toSet()

        val filteredNewVtableSlots = newVtableSlots
                .filterNot { inheritedVtableSlotsSet.contains(it.function to it.bridgeDirections) }
                .distinctBy { it.function to it.bridgeDirections }
                .filter { it.function.isOverridable }

        DEBUG_OUTPUT(0) {
            println()
            println("INHERITED vTable slots:")
            inheritedVtableSlots.forEach { println("    ${it.overriddenFunction.descriptor} -> ${it.function.descriptor}") }

            println()
            println("MY OWN vTable slots:")
            filteredNewVtableSlots.forEach { println("    ${it.overriddenFunction.descriptor} -> ${it.function.descriptor}") }
        }

        inheritedVtableSlots + filteredNewVtableSlots.sortedBy { it.overriddenFunction.uniqueId }
    }

    fun vtableIndex(function: IrSimpleFunction): Int {
        val bridgeDirections = function.target.bridgeDirectionsTo(function)
        val index = vtableEntries.indexOfFirst { it.function == function && it.bridgeDirections == bridgeDirections }
        if (index < 0) throw Error(function.toString() + " not in vtable of " + irClass.toString())
        return index
    }

    val methodTableEntries: List<OverriddenFunctionInfo> by lazy {
        irClass.sortedOverridableOrOverridingMethods
                .flatMap { method -> method.allOverriddenFunctions.map { OverriddenFunctionInfo(method, it) } }
                .filter { it.canBeCalledVirtually }
                .distinctBy { it.overriddenFunction.uniqueId }
                .sortedBy { it.overriddenFunction.uniqueId }
        // TODO: probably method table should contain all accessible methods to improve binary compatibility
    }

    private val IrClass.sortedOverridableOrOverridingMethods: List<IrSimpleFunction>
        get() =
            this.simpleFunctions()
                    .filter { (it.isOverridable || it.overriddenSymbols.isNotEmpty())
                               && it.bridgeTarget == null }
                    .sortedBy { it.uniqueId }

    private val functionIds = mutableMapOf<IrFunction, Long>()

    private val IrFunction.uniqueId get() = functionIds.getOrPut(this) { functionName.localHash.value }
}