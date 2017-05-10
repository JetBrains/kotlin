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

import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.isRepresentedAs
import org.jetbrains.kotlin.backend.konan.isValueType
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.backend.konan.llvm.isExported
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
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
        @Suppress("UNCHECKED_CAST")
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

internal val FunctionDescriptor.isSuspendFunctionInvoke: Boolean
    get() {
        val dispatchReceiver = dispatchReceiverParameter
                ?: return false

        return dispatchReceiver.type.isSuspendFunctionType &&
                this.isOperator && this.name == OperatorNameConventions.INVOKE
    }

internal fun ClassDescriptor.isUnit() = this.defaultType.isUnit()

internal val <T : CallableMemberDescriptor> T.allOverriddenDescriptors: List<T>
    get() {
        val result = mutableListOf<T>()
        fun traverse(descriptor: T) {
            result.add(descriptor)
            @Suppress("UNCHECKED_CAST")
            descriptor.overriddenDescriptors.forEach { traverse(it as T) }
        }
        traverse(this)
        return result
    }

internal val ClassDescriptor.sortedContributedMethods: List<FunctionDescriptor>
    get () = unsubstitutedMemberScope.sortedContributedMethods

internal val ClassDescriptor.contributedMethods: List<FunctionDescriptor>
    get () = unsubstitutedMemberScope.contributedMethods

internal val MemberScope.sortedContributedMethods: List<FunctionDescriptor>
    get () = contributedMethods.sortedBy {
            it.functionName.localHash.value
    }

internal val MemberScope.contributedMethods: List<FunctionDescriptor>
    get () {
        val contributedDescriptors = this.getContributedDescriptors()

        val functions = contributedDescriptors.filterIsInstance<FunctionDescriptor>()

        val properties = contributedDescriptors.filterIsInstance<PropertyDescriptor>()
        val getters = properties.mapNotNull { it.getter }
        val setters = properties.mapNotNull { it.setter }

        return functions + getters + setters
    }

fun ClassDescriptor.isAbstract() = this.modality == Modality.SEALED || this.modality == Modality.ABSTRACT
        || this.kind == ClassKind.ENUM_CLASS

internal fun FunctionDescriptor.hasValueTypeAt(index: Int): Boolean {
    when (index) {
        0 -> return !isSuspend && returnType.let { it != null && it.isValueType() }
        1 -> return extensionReceiverParameter.let { it != null && it.type.isValueType() }
        else -> return this.valueParameters[index - 2].type.isValueType()
    }
}

internal fun FunctionDescriptor.hasReferenceAt(index: Int): Boolean {
    when (index) {
        0 -> return isSuspend || returnType.let { it != null && !it.isValueType() }
        1 -> return extensionReceiverParameter.let { it != null && !it.type.isValueType() }
        else -> return !this.valueParameters[index - 2].type.isValueType()
    }
}

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

private fun FunctionDescriptor.bridgeDirectionToAt(target: FunctionDescriptor, index: Int): BridgeDirection {
    when {
        hasValueTypeAt(index) && target.hasReferenceAt(index) -> return BridgeDirection.FROM_VALUE_TYPE
        hasReferenceAt(index) && target.hasValueTypeAt(index) -> return BridgeDirection.TO_VALUE_TYPE
        else -> return BridgeDirection.NOT_NEEDED
    }
}

internal class BridgeDirections(val array: Array<BridgeDirection>) {
    constructor(parametersCount: Int): this(Array<BridgeDirection>(parametersCount + 2, { BridgeDirection.NOT_NEEDED }))

    fun allNotNeeded(): Boolean = array.all { it == BridgeDirection.NOT_NEEDED }

    override fun toString(): String {
        val result = StringBuilder()
        array.forEach {
            result.append(when (it) {
                BridgeDirection.FROM_VALUE_TYPE -> 'U' // unbox
                BridgeDirection.TO_VALUE_TYPE -> 'B' // box
                BridgeDirection.NOT_NEEDED -> 'N' // none
            })
        }
        return result.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BridgeDirections) return false

        return array.size == other.array.size
                && array.indices.all { array[it] == other.array[it] }
    }

    override fun hashCode(): Int {
        var result = 0
        array.forEach { result = result * 31 + it.ordinal }
        return result
    }
}

internal fun FunctionDescriptor.bridgeDirectionsTo(overriddenDescriptor: FunctionDescriptor): BridgeDirections {
    val ourDirections = BridgeDirections(this.valueParameters.size)
    for (index in ourDirections.array.indices)
        ourDirections.array[index] = this.bridgeDirectionToAt(overriddenDescriptor, index)

    val target = this.target
    if (!kind.isReal && modality != Modality.ABSTRACT
            && OverridingUtil.overrides(target, overriddenDescriptor)
            && ourDirections == target.bridgeDirectionsTo(overriddenDescriptor)) {
        // Bridge is inherited from superclass.
        return BridgeDirections(this.valueParameters.size)
    }

    return ourDirections
}


internal fun DeclarationDescriptor.getMemberScope(): MemberScope {
        val containingScope = when (this) {
            is ClassDescriptor -> this.unsubstitutedMemberScope
            is PackageViewDescriptor -> this.memberScope
            else -> error("Unexpected member scope: $containingDeclaration")
        }
        return containingScope
}

// It is possible to declare "external inline fun",
// but it doesn't have much sense for native,
// since externals don't have IR bodies.
// Enforce inlining of constructors annotated with @InlineConstructor.

private val inlineConstructor = FqName("konan.internal.InlineConstructor")

internal val FunctionDescriptor.needsInlining: Boolean
    get() {
        val inlineConstructor = annotations.hasAnnotation(inlineConstructor)
        if (inlineConstructor) return true
        return (this.isInline && !this.isExternal)
    }

internal val FunctionDescriptor.needsSerializedIr: Boolean 
    get() = (this.needsInlining && this.isExported())

