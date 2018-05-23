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

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.allOverriddenDescriptors
import org.jetbrains.kotlin.backend.konan.descriptors.isArray
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal abstract class ObjCExportMapper {
    abstract fun getCategoryMembersFor(descriptor: ClassDescriptor): List<CallableMemberDescriptor>
    val maxFunctionTypeParameterCount get() = KONAN_FUNCTION_INTERFACES_MAX_PARAMETERS
    abstract fun isSpecialMapped(descriptor: ClassDescriptor): Boolean

    private val methodBridgeCache = mutableMapOf<FunctionDescriptor, MethodBridge>()

    fun bridgeMethod(descriptor: FunctionDescriptor): MethodBridge = methodBridgeCache.getOrPut(descriptor) {
        bridgeMethodImpl(descriptor)
    }
}

private fun ObjCExportMapper.isRepresentedAsObjCInterface(descriptor: ClassDescriptor): Boolean =
        !descriptor.isInterface && !isSpecialMapped(descriptor)

internal fun ObjCExportMapper.getClassIfCategory(descriptor: CallableMemberDescriptor): ClassDescriptor? {
    if (descriptor.dispatchReceiverParameter != null) return null

    val extensionReceiverType = descriptor.extensionReceiverParameter?.type ?: return null

    if (extensionReceiverType.isObjCObjectType()) return null

    val erasedClass = extensionReceiverType.getErasedTypeClass()
    return if (this.isRepresentedAsObjCInterface(erasedClass)) {
        erasedClass
    } else {
        // E.g. receiver is protocol, or some type with custom mapping.
        null
    }
}

internal fun ObjCExportMapper.shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean =
        descriptor.isEffectivelyPublicApi && !descriptor.isSuspend && !descriptor.isExpect

internal fun ObjCExportMapper.shouldBeExposed(descriptor: ClassDescriptor): Boolean =
        descriptor.isEffectivelyPublicApi && !descriptor.defaultType.isObjCObjectType() && when (descriptor.kind) {
            ClassKind.CLASS, ClassKind.INTERFACE, ClassKind.ENUM_CLASS, ClassKind.OBJECT -> true
            ClassKind.ENUM_ENTRY, ClassKind.ANNOTATION_CLASS -> false
        } && !descriptor.isExpect && !isSpecialMapped(descriptor)

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

internal fun ObjCExportMapper.isObjCProperty(property: PropertyDescriptor): Boolean =
        property.extensionReceiverParameter == null || getClassIfCategory(property) != null

internal fun ObjCExportMapper.doesThrow(method: FunctionDescriptor): Boolean = method.allOverriddenDescriptors.any {
    it.overriddenDescriptors.isEmpty() && it.annotations.hasAnnotation(KonanBuiltIns.FqNames.throws)
}

private fun ObjCExportMapper.bridgeType(kotlinType: KotlinType): TypeBridge {
    val valueType = kotlinType.correspondingValueType
            ?: return ReferenceBridge

    val objCValueType = ObjCValueType.values().singleOrNull { it.kotlinValueType == valueType }
            ?: error("Can't produce $kotlinType to framework API")

    return ValueTypeBridge(objCValueType)
}

private fun ObjCExportMapper.bridgeParameter(parameter: ParameterDescriptor): MethodBridgeValueParameter =
        MethodBridgeValueParameter.Mapped(bridgeType(parameter.type))

private fun ObjCExportMapper.bridgeReturnType(
        descriptor: FunctionDescriptor,
        valueParameters: MutableList<MethodBridgeValueParameter>,
        convertExceptionsToErrors: Boolean
): MethodBridge.ReturnValue {
    val returnType = descriptor.returnType!!
    return when {
        descriptor is ConstructorDescriptor -> if (descriptor.constructedClass.isArray) {
            MethodBridge.ReturnValue.Instance.FactoryResult
        } else {
            MethodBridge.ReturnValue.Instance.InitResult
        }.let {
            if (convertExceptionsToErrors) MethodBridge.ReturnValue.WithError.RefOrNull(it) else it
        }

        descriptor.containingDeclaration == descriptor.builtIns.any && descriptor.name.asString() == "hashCode" -> {
            assert(!convertExceptionsToErrors)
            MethodBridge.ReturnValue.HashCode
        }

        descriptor is PropertyGetterDescriptor -> {
            assert(!convertExceptionsToErrors)
            MethodBridge.ReturnValue.Mapped(bridgePropertyType(descriptor.correspondingProperty))
        }

        returnType.isUnit() -> if (convertExceptionsToErrors) {
            MethodBridge.ReturnValue.WithError.Success
        } else {
            MethodBridge.ReturnValue.Void
        }

        else -> {
            val returnTypeBridge = bridgeType(returnType)
            if (convertExceptionsToErrors) {
                if (returnTypeBridge is ReferenceBridge && !TypeUtils.isNullableType(returnType)) {
                    MethodBridge.ReturnValue.WithError.RefOrNull(MethodBridge.ReturnValue.Mapped(returnTypeBridge))
                } else {
                    valueParameters += MethodBridgeValueParameter.KotlinResultOutParameter(returnTypeBridge)
                    MethodBridge.ReturnValue.WithError.Success
                }
            } else {
                MethodBridge.ReturnValue.Mapped(returnTypeBridge)
            }
        }
    }
}

private fun ObjCExportMapper.bridgeMethodImpl(descriptor: FunctionDescriptor): MethodBridge {
    assert(isBaseMethod(descriptor))

    val convertExceptionsToErrors = this.doesThrow(descriptor)

    val kotlinParameters = descriptor.allParameters.iterator()

    val isTopLevel = isTopLevel(descriptor)

    val receiver = if (descriptor is ConstructorDescriptor && descriptor.constructedClass.isArray) {
        kotlinParameters.next()
        MethodBridgeReceiver.Factory
    } else if (isTopLevel) {
        MethodBridgeReceiver.Static
    } else {
        kotlinParameters.next()
        MethodBridgeReceiver.Instance
    }

    val valueParameters = mutableListOf<MethodBridgeValueParameter>()
    kotlinParameters.forEach {
        valueParameters += bridgeParameter(it)
    }

    val returnBridge = bridgeReturnType(descriptor, valueParameters, convertExceptionsToErrors)
    if (convertExceptionsToErrors) {
        valueParameters += MethodBridgeValueParameter.ErrorOutParameter
    }

    return MethodBridge(returnBridge, receiver, valueParameters)
}

internal fun ObjCExportMapper.bridgePropertyType(descriptor: PropertyDescriptor): TypeBridge {
    assert(isBaseProperty(descriptor))

    return bridgeType(descriptor.type)
}