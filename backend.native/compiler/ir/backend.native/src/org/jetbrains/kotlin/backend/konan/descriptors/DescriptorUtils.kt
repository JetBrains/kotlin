package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.isRepresentedAs
import org.jetbrains.kotlin.backend.konan.isValueType
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * List of all implemented interfaces (including those which implemented by a super class)
 */
internal val ClassDescriptor.implementedInterfaces: List<ClassDescriptor>
    get() {
        val superClassImplementedInterfaces = this.getSuperClassNotAny()?.implementedInterfaces ?: emptyList()
        val superInterfaces = this.getSuperInterfaces()
        val superInterfacesImplementedInterfaces = superInterfaces.flatMap { it.implementedInterfaces }
        return (superClassImplementedInterfaces +
                superInterfacesImplementedInterfaces +
                superInterfaces).distinctBy { it.classId }
    }


/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal fun <T : CallableMemberDescriptor> T.resolveFakeOverride(): T {
    if (this.kind.isReal) {
        return this
    } else {
        val overridden = OverridingUtil.getOverriddenDeclarations(this)
        val filtered = OverridingUtil.filterOutOverridden(overridden)
        // TODO: is it correct to take first?
        return filtered.first { it.modality != Modality.ABSTRACT } as T
    }
}

private val intrinsicAnnotation = FqName("konan.internal.Intrinsic")

// TODO: check it is external?
internal val FunctionDescriptor.isIntrinsic: Boolean
    get() = this.annotations.findAnnotation(intrinsicAnnotation) != null

private val intrinsicTypes = setOf(
        "kotlin.Boolean", "kotlin.Char",
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

private val konanInternalPackageName = FqName.fromSegments(listOf("konan", "internal"))

/**
 * @return `konan.internal` member scope
 */
internal val KonanBuiltIns.konanInternal: MemberScope
    get() = this.builtInsModule.getPackage(konanInternalPackageName).memberScope

/**
 * @return built-in class `konan.internal.$name` or
 * `null` if no such class is available (e.g. when compiling `link` test without stdlib).
 *
 * TODO: remove this workaround after removing compilation without stdlib.
 */
internal fun KonanBuiltIns.getKonanInternalClassOrNull(name: String): ClassDescriptor? {
    val classifier = konanInternal.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
    return classifier as? ClassDescriptor
}

/**
 * @return built-in class `konan.internal.$name`
 */
internal fun KonanBuiltIns.getKonanInternalClass(name: String): ClassDescriptor =
        getKonanInternalClassOrNull(name) ?: TODO(name)

internal fun KonanBuiltIns.getKonanInternalFunctions(name: String): List<FunctionDescriptor> {
    return konanInternal.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).toList()
}

internal fun KotlinType.isUnboundCallableReference() = this.isRepresentedAs(ValueType.UNBOUND_CALLABLE_REFERENCE)

internal val CallableDescriptor.allValueParameters: List<ParameterDescriptor>
    get() {
        val receivers = mutableListOf<ParameterDescriptor>()

        if (this is ConstructorDescriptor)
            receivers.add(this.constructedClass.thisAsReceiverParameter)

        val dispatchReceiverParameter = this.dispatchReceiverParameter
        if (dispatchReceiverParameter != null)
            receivers.add(dispatchReceiverParameter)

        val extensionReceiverParameter = this.extensionReceiverParameter
        if (extensionReceiverParameter != null)
            receivers.add(extensionReceiverParameter)

        return receivers + this.valueParameters
    }

internal val KotlinType.isFunctionOrKFunctionType: Boolean
    get() {
        val kind = constructor.declarationDescriptor?.getFunctionalClassKind()
        return kind == FunctionClassDescriptor.Kind.Function || kind == FunctionClassDescriptor.Kind.KFunction
    }

internal val KotlinType.isKFunctionType: Boolean
    get() {
        val kind = constructor.declarationDescriptor?.getFunctionalClassKind()
        return kind == FunctionClassDescriptor.Kind.KFunction
    }

internal val FunctionDescriptor.isFunctionInvoke: Boolean
    get() {
        val dispatchReceiver = dispatchReceiverParameter ?: return false
        assert(!dispatchReceiver.type.isKFunctionType)

        return dispatchReceiver.type.isFunctionType &&
                this.isOperator && this.name == OperatorNameConventions.INVOKE
    }

internal fun ClassDescriptor.isUnit() = this.defaultType.isUnit()

internal val <T : CallableMemberDescriptor> T.allOverriddenDescriptors: List<T>
    get() {
        val result = mutableListOf<T>()
        fun traverse(descriptor: T) {
            result.add(descriptor)
            descriptor.overriddenDescriptors.forEach { traverse(it as T) }
        }
        traverse(this)
        return result
    }

internal val ClassDescriptor.contributedMethods: List<FunctionDescriptor>
    get () {
        val contributedDescriptors = unsubstitutedMemberScope.getContributedDescriptors()
        // (includes declarations from supers)

        val functions = contributedDescriptors.filterIsInstance<FunctionDescriptor>()

        val properties = contributedDescriptors.filterIsInstance<PropertyDescriptor>()
        val getters = properties.mapNotNull { it.getter }
        val setters = properties.mapNotNull { it.setter }

        val allMethods = (functions + getters + setters).sortedBy {
            it.functionName.localHash.value
        }

        return allMethods
    }

fun ClassDescriptor.isAbstract() = this.modality == Modality.SEALED || this.modality == Modality.ABSTRACT
        || this.kind == ClassKind.ENUM_CLASS

internal fun FunctionDescriptor.hasValueTypeAt(index: Int): Boolean {
    when (index) {
        0 -> return returnType.let { it != null && it.isValueType() }
        1 -> return extensionReceiverParameter.let { it != null && it.type.isValueType() }
        else -> return this.valueParameters[index - 2].type.isValueType()
    }
}

internal fun FunctionDescriptor.hasReferenceAt(index: Int): Boolean {
    when (index) {
        0 -> return returnType.let { it != null && !it.isValueType() }
        1 -> return extensionReceiverParameter.let { it != null && !it.type.isValueType() }
        else -> return !this.valueParameters[index - 2].type.isValueType()
    }
}

private fun FunctionDescriptor.overridesFunWithReferenceAt(index: Int)
        = allOverriddenDescriptors.any { it.original.hasReferenceAt(index) }

private fun FunctionDescriptor.overridesFunWithValueTypeAt(index: Int)
        = allOverriddenDescriptors.any { it.original.hasValueTypeAt(index) }

private fun FunctionDescriptor.needBridgeToAt(target: FunctionDescriptor, index: Int)
        = hasValueTypeAt(index) xor target.hasValueTypeAt(index)

internal fun FunctionDescriptor.needBridgeTo(target: FunctionDescriptor)
        = (0..this.valueParameters.size + 1).any { needBridgeToAt(target, it) }

internal val FunctionDescriptor.target: FunctionDescriptor
    get() = (if (modality == Modality.ABSTRACT) this else resolveFakeOverride()).original

internal enum class BridgeDirection {
    NOT_NEEDED,
    FROM_VALUE_TYPE,
    TO_VALUE_TYPE
}

private fun FunctionDescriptor.bridgeDirectionAt(index: Int) : BridgeDirection {
    if (kind.isReal) {
        if (hasValueTypeAt(index) && overridesFunWithReferenceAt(index))
            return BridgeDirection.FROM_VALUE_TYPE
        return BridgeDirection.NOT_NEEDED
    }

    val target = this.target
    return when {
        hasValueTypeAt(index) && target.hasValueTypeAt(index) && overridesFunWithReferenceAt(index) -> BridgeDirection.FROM_VALUE_TYPE
        hasValueTypeAt(index) && target.hasReferenceAt(index) && overridesFunWithValueTypeAt(index) -> BridgeDirection.TO_VALUE_TYPE
        else -> BridgeDirection.NOT_NEEDED
    }
}

private fun bridgesEqual(first: Array<BridgeDirection>, second: Array<BridgeDirection>)
        = first.indices.none { first[it] != second[it] }

internal fun Array<BridgeDirection>.allNotNeeded() = this.all { it == BridgeDirection.NOT_NEEDED }

internal val FunctionDescriptor.bridgeDirections: Array<BridgeDirection>
    get() {
        val ourDirections = Array<BridgeDirection>(this.valueParameters.size + 2, { BridgeDirection.NOT_NEEDED })
        if (modality == Modality.ABSTRACT)
            return ourDirections
        for (index in ourDirections.indices)
            ourDirections[index] = this.bridgeDirectionAt(index)

        if (!kind.isReal && bridgesEqual(ourDirections, this.target.bridgeDirections)) {
            // Bridge is inherited from supers
            for (index in ourDirections.indices)
                ourDirections[index] = BridgeDirection.NOT_NEEDED
        }

        return ourDirections
    }
