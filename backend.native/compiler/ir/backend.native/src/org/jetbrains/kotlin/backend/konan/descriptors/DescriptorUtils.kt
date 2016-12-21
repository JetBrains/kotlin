package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
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
        getKonanInternalClassOrNull(name)!!

internal fun KonanBuiltIns.getKonanInternalFunctions(name: String): List<FunctionDescriptor> {
    return konanInternal.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).toList()
}

private val UNBOUND_CALLABLE_REFERENCE = "UnboundCallableReference"

/**
 * `konan.internal.UnboundCallableReference` type or `null` if it is not available.
 */
internal val KonanBuiltIns.unboundCallableReferenceTypeOrNull: KotlinType?
    get() = this.getKonanInternalClassOrNull(UNBOUND_CALLABLE_REFERENCE)?.defaultType

internal fun KotlinType.isUnboundCallableReference(): Boolean {
    val classDescriptor = TypeUtils.getClassDescriptor(this) ?: return false

    return !this.isMarkedNullable &&
            classDescriptor.fqNameUnsafe.asString() == "konan.internal.$UNBOUND_CALLABLE_REFERENCE"
}

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

internal val FunctionDescriptor.isFunctionInvoke: Boolean
    get() {
        val dispatchReceiver = dispatchReceiverParameter ?: return false

        return dispatchReceiver.type.isFunctionType &&
                this.isOperator && this.name == OperatorNameConventions.INVOKE
    }
