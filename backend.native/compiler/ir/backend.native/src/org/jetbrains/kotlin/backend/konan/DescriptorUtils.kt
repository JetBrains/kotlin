package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.*

/**
 * List of all implemented interfaces (including those which implemented by a super class)
 */
internal val ClassDescriptor.implementedInterfaces: List<ClassDescriptor>
    get() {
        val superClassImplementedInterfaces = this.getSuperClassNotAny()?.implementedInterfaces ?: emptyList()
        return (superClassImplementedInterfaces + this.getSuperInterfaces()).distinctBy { it.classId }
    }


/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal val FunctionDescriptor.implementation: FunctionDescriptor
    get() {
        if (this.kind.isReal) {
            return this
        } else {
            val overridden = OverridingUtil.getOverriddenDeclarations(this)
            val filtered = OverridingUtil.filterOutOverridden(overridden)
            return filtered.first { it.modality != Modality.ABSTRACT } as FunctionDescriptor
        }
    }

private val intrinsicTypes = setOf(
        "kotlin.Unit",
        "kotlin.Boolean", "kotlin.Char",
        "kotlin.Number",
        "kotlin.Byte", "kotlin.Short",
        "kotlin.Int", "kotlin.Long",
        "kotlin.Float", "kotlin.Double"
)

private val arrayTypes = setOf(
        "kotlin.Array",
        "kotlin.ByteArray",
        "kotlin.CharArray",
        "kotlin.ShortArray",
        "kotlin.IntArray",
        "kotlin.LongArray",
        "kotlin.FloatArray",
        "kotlin.DoubleArray",
        "kotlin.BooleanArray"
)

internal val ClassDescriptor.isIntrinsic: Boolean
    get() = this.fqNameSafe.asString() in intrinsicTypes


internal val ClassDescriptor.isArray: Boolean
    get() = this.fqNameSafe.asString() in arrayTypes


internal val ClassDescriptor.isInterface: Boolean
    get() = (this.kind == ClassKind.INTERFACE)

internal val CallableDescriptor.allValueParameters: List<ParameterDescriptor>
    get() {
        val constructorReceiver = if (this is ConstructorDescriptor) {
            this.constructedClass.thisAsReceiverParameter
        } else {
            null
        }

        val receivers = listOf(
                constructorReceiver,
                this.dispatchReceiverParameter,
                this.extensionReceiverParameter).filterNotNull()

        return receivers + this.valueParameters
    }