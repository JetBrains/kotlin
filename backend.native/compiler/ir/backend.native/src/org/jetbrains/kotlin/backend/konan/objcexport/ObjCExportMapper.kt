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

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.correspondingValueType
import org.jetbrains.kotlin.backend.konan.descriptors.isArray
import org.jetbrains.kotlin.backend.konan.isObjCObjectType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal interface ObjCExportMapper {
    fun isRepresentedAsObjCInterface(descriptor: ClassDescriptor): Boolean
    fun getCategoryMembersFor(descriptor: ClassDescriptor): List<CallableMemberDescriptor>
}

internal fun ObjCExportMapper.getClassIfCategory(descriptor: CallableMemberDescriptor): ClassDescriptor? {
    if (descriptor.dispatchReceiverParameter != null) return null

    val extensionReceiverType = descriptor.extensionReceiverParameter?.type ?: return null

    val erasedClass = extensionReceiverType.getErasedTypeClass()
    return if (this.isRepresentedAsObjCInterface(erasedClass)) {
        erasedClass
    } else {
        // E.g. receiver is protocol, or some type with custom mapping.
        null
    }
}

internal fun ObjCExportMapper.shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean =
        descriptor.isEffectivelyPublicApi && !descriptor.isSuspend

internal fun ObjCExportMapper.shouldBeExposed(descriptor: ClassDescriptor): Boolean =
        descriptor.isEffectivelyPublicApi && !descriptor.defaultType.isObjCObjectType() && when (descriptor.kind) {
            ClassKind.CLASS, ClassKind.INTERFACE, ClassKind.ENUM_CLASS, ClassKind.OBJECT -> true
            ClassKind.ENUM_ENTRY, ClassKind.ANNOTATION_CLASS -> false
        }

private fun ObjCExportMapper.isBase(descriptor: CallableMemberDescriptor): Boolean =
        descriptor.overriddenDescriptors.all { !shouldBeExposed(it) }
        // e.g. it is not `override`, or overrides only unexposed methods.

internal fun ObjCExportMapper.isBaseMethod(descriptor: FunctionDescriptor) =
        this.isBase(descriptor)

internal fun ObjCExportMapper.getBaseMethods(descriptor: FunctionDescriptor): List<FunctionDescriptor> =
        if (isBaseMethod(descriptor)) {
            listOf(descriptor)
        } else {
            descriptor.overriddenDescriptors.filter { shouldBeExposed(it) }
                    .flatMap { getBaseMethods(it.original)}
                    .distinct()
        }

internal fun ObjCExportMapper.isBaseProperty(descriptor: PropertyDescriptor) =
        isBase(descriptor)

internal fun ObjCExportMapper.getBaseProperties(descriptor: PropertyDescriptor): List<PropertyDescriptor> =
        if (isBaseProperty(descriptor)) {
            listOf(descriptor)
        } else {
            descriptor.overriddenDescriptors
                    .flatMap { getBaseProperties(it.original) }
                    .distinct()
        }

internal tailrec fun KotlinType.getErasedTypeClass(): ClassDescriptor =
        TypeUtils.getClassDescriptor(this) ?: this.constructor.supertypes.first().getErasedTypeClass()

internal fun ObjCExportMapper.isTopLevel(descriptor: CallableMemberDescriptor): Boolean =
        descriptor.containingDeclaration !is ClassDescriptor && this.getClassIfCategory(descriptor) == null

internal fun ObjCExportMapper.objCValueParameters(method: FunctionDescriptor): List<ParameterDescriptor> =
        when {
            method is ConstructorDescriptor ->
                listOfNotNull(method.dispatchReceiverParameter) + method.valueParameters

            getClassIfCategory(method) == null ->
                listOfNotNull(method.extensionReceiverParameter) + method.valueParameters

            else -> method.valueParameters
        }

internal fun ObjCExportMapper.isObjCProperty(property: PropertyDescriptor): Boolean =
        this.objCValueParameters(property.getter!!).isEmpty() && // Which is false e.g. if it has two receivers.
                !this.isTopLevel(property) // Because Objective-C has no class (e.g. static) properties.

// TODO: generalize type bridges to support such things as selectors, ignored class method receivers etc.

internal sealed class ReturnableTypeBridge
internal object VoidBridge : ReturnableTypeBridge()
internal sealed class TypeBridge : ReturnableTypeBridge()
internal object ReferenceBridge : TypeBridge()
internal data class ValueTypeBridge(val objCValueType: ObjCValueType) : TypeBridge()
internal object HashCodeBridge : TypeBridge()

internal data class MethodBridge(
        val returnBridge: ReturnableTypeBridge,
        val paramBridges: List<TypeBridge>,
        val isKotlinTopLevel: Boolean = false
)

private fun ObjCExportMapper.bridgeType(kotlinType: KotlinType): TypeBridge {
    val valueType = kotlinType.correspondingValueType
            ?: return ReferenceBridge

    val objCValueType = ObjCValueType.values().singleOrNull { it.kotlinValueType == valueType }
            ?: error("Can't produce $kotlinType to framework API")

    return ValueTypeBridge(objCValueType)
}

private fun ObjCExportMapper.bridgeReturnType(kotlinType: KotlinType): ReturnableTypeBridge = if (kotlinType.isUnit()) {
    VoidBridge
} else {
    bridgeType(kotlinType)
}

internal fun ObjCExportMapper.bridgeReturnType(descriptor: FunctionDescriptor): ReturnableTypeBridge {
    val returnType = descriptor.returnType!!
    return when {
        descriptor.containingDeclaration == descriptor.builtIns.any && descriptor.name.asString() == "hashCode" ->
            HashCodeBridge

        descriptor is PropertyGetterDescriptor -> bridgePropertyType(descriptor.correspondingProperty)

        else -> bridgeReturnType(returnType)
    }
}

internal fun ObjCExportMapper.bridgeMethod(descriptor: FunctionDescriptor): MethodBridge {
    assert(isBaseMethod(descriptor))

    val returnBridge = bridgeReturnType(descriptor)

    val paramBridges = mutableListOf<TypeBridge>()

    if (descriptor is ConstructorDescriptor) {
        if (descriptor.constructedClass.isArray) {
            // Generated as class factory method.
            paramBridges += ReferenceBridge // Receiver of class method.
        } else {
            // Generated as Objective-C instance init method.
            paramBridges += ReferenceBridge // Receiver of init method.
        }
    }

    val isTopLevel = isTopLevel(descriptor)
    if (isTopLevel) {
        paramBridges += ReferenceBridge
    }

    descriptor.explicitParameters.mapTo(paramBridges) { bridgeType(it.type) }

    return MethodBridge(returnBridge, paramBridges, isKotlinTopLevel = isTopLevel)
}

internal fun ObjCExportMapper.bridgePropertyType(descriptor: PropertyDescriptor): TypeBridge {
    assert(isBaseProperty(descriptor))

    return bridgeType(descriptor.type)
}

internal enum class ObjCValueType(
        val kotlinValueType: ValueType, // It is here for simplicity.
        val encoding: String
) {

    BOOL(ValueType.BOOLEAN, "c"),
    CHAR(ValueType.BYTE, "c"),
    UNSIGNED_SHORT(ValueType.CHAR, "S"),
    SHORT(ValueType.SHORT, "s"),
    INT(ValueType.INT, "i"),
    LONG_LONG(ValueType.LONG, "q"),
    FLOAT(ValueType.FLOAT, "f"),
    DOUBLE(ValueType.DOUBLE, "d")

    ;

    // UNSIGNED_SHORT -> unsignedShort
    val nsNumberName = this.name.split('_').mapIndexed { index, s ->
        val lower = s.toLowerCase()
        if (index > 0) lower.capitalize() else lower
    }.joinToString("")

    val nsNumberValueSelector get() = "${nsNumberName}Value"
    val nsNumberFactorySelector get() = "numberWith${nsNumberName.capitalize()}:"
}
