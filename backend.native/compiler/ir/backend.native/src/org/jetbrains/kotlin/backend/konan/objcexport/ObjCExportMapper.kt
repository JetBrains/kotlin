/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.allOverriddenDescriptors
import org.jetbrains.kotlin.backend.konan.descriptors.isArray
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.deprecation.Deprecation
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal class ObjCExportMapper(
        internal val deprecationResolver: DeprecationResolver? = null,
        private val local: Boolean = false
) {
    fun getCustomTypeMapper(descriptor: ClassDescriptor): CustomTypeMapper? = CustomTypeMappers.getMapper(descriptor)

    val hiddenTypes: Set<ClassId> get() = CustomTypeMappers.hiddenTypes

    fun isSpecialMapped(descriptor: ClassDescriptor): Boolean {
        // TODO: this method duplicates some of the [ObjCExportTranslatorImpl.mapReferenceType] logic.
        return KotlinBuiltIns.isAny(descriptor) ||
                descriptor.getAllSuperClassifiers().any { it is ClassDescriptor && CustomTypeMappers.hasMapper(it) }
    }

    private val methodBridgeCache = mutableMapOf<FunctionDescriptor, MethodBridge>()

    fun bridgeMethod(descriptor: FunctionDescriptor): MethodBridge = if (local) {
        bridgeMethodImpl(descriptor)
    } else {
        methodBridgeCache.getOrPut(descriptor) {
            bridgeMethodImpl(descriptor)
        }
    }
}

internal fun ObjCExportMapper.getClassIfCategory(descriptor: CallableMemberDescriptor): ClassDescriptor? {
    if (descriptor.dispatchReceiverParameter != null) return null

    val extensionReceiverType = descriptor.extensionReceiverParameter?.type ?: return null

    return getClassIfCategory(extensionReceiverType)
}

internal fun ObjCExportMapper.getClassIfCategory(extensionReceiverType: KotlinType): ClassDescriptor? {
    // FIXME: this code must rely on type mapping instead of copying its logic.

    if (extensionReceiverType.isObjCObjectType()) return null

    val erasedClass = extensionReceiverType.getErasedTypeClass()
    return if (!erasedClass.isInterface && !erasedClass.isInlined() && !this.isSpecialMapped(erasedClass)) {
        erasedClass
    } else {
        // E.g. receiver is protocol, or some type with custom mapping.
        null
    }
}

// Note: partially duplicated in ObjCExportLazyImpl.translateTopLevels.
internal fun ObjCExportMapper.shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean =
        descriptor.isEffectivelyPublicApi && !descriptor.isSuspend && !descriptor.isExpect &&
                !isHiddenByDeprecation(descriptor)

internal fun ObjCExportMapper.shouldBeExposed(descriptor: ClassDescriptor): Boolean =
        shouldBeVisible(descriptor) && !isSpecialMapped(descriptor) && !descriptor.defaultType.isObjCObjectType()

private fun ObjCExportMapper.isHiddenByDeprecation(descriptor: CallableMemberDescriptor): Boolean {
    // Note: ObjCExport generally expect overrides of exposed methods to be exposed.
    // So don't hide a "deprecated hidden" method which overrides non-hidden one:
    if (deprecationResolver != null && deprecationResolver.isDeprecatedHidden(descriptor) &&
            descriptor.overriddenDescriptors.all { isHiddenByDeprecation(it) }) {
        return true
    }

    // Note: ObjCExport expects members of unexposed classes to be unexposed too.
    // So hide a declaration if it is from a hidden class:
    val containingDeclaration = descriptor.containingDeclaration
    if (containingDeclaration is ClassDescriptor && isHiddenByDeprecation(containingDeclaration)) {
        return true
    }

    return false
}

internal fun ObjCExportMapper.getDeprecation(descriptor: DeclarationDescriptor): Deprecation? {
    deprecationResolver?.getDeprecations(descriptor).orEmpty().maxBy {
        when (it.deprecationLevel) {
            DeprecationLevelValue.WARNING -> 1
            DeprecationLevelValue.ERROR -> 2
            DeprecationLevelValue.HIDDEN -> 3
        }
    }?.let { return it }

    (descriptor as? ConstructorDescriptor)?.let {
        // Note: a deprecation can't be applied to a class itself when generating header
        // since the class can be referred from the header.
        // Apply class deprecations to its constructors instead:
        return getDeprecation(it.constructedClass)
    }

    return null
}

private fun ObjCExportMapper.isHiddenByDeprecation(descriptor: ClassDescriptor): Boolean {
    if (deprecationResolver == null) return false
    if (deprecationResolver.isDeprecatedHidden(descriptor)) return true

    // Note: ObjCExport requires super class of exposed class to be exposed.
    // So hide a class if its super class is hidden:
    val superClass = descriptor.getSuperClassNotAny()
    if (superClass != null && isHiddenByDeprecation(superClass)) {
        return true
    }

    // Note: ObjCExport requires enclosing class of exposed class to be exposed.
    // Also in Kotlin hidden class members (including other classes) aren't directly accessible.
    // So hide a class if its enclosing class is hidden:
    val containingDeclaration = descriptor.containingDeclaration
    if (containingDeclaration is ClassDescriptor && isHiddenByDeprecation(containingDeclaration)) {
        return true
    }

    return false
}

// Note: the logic is partially duplicated in ObjCExportLazyImpl.translateClasses.
internal fun ObjCExportMapper.shouldBeVisible(descriptor: ClassDescriptor): Boolean =
        descriptor.isEffectivelyPublicApi && when (descriptor.kind) {
        ClassKind.CLASS, ClassKind.INTERFACE, ClassKind.ENUM_CLASS, ClassKind.OBJECT -> true
        ClassKind.ENUM_ENTRY, ClassKind.ANNOTATION_CLASS -> false
    } && !descriptor.isExpect && !descriptor.isInlined() && !isHiddenByDeprecation(descriptor)

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
    it.overriddenDescriptors.isEmpty() && it.annotations.hasAnnotation(KonanFqNames.throws)
}

private fun ObjCExportMapper.bridgeType(
        kotlinType: KotlinType
): TypeBridge = kotlinType.unwrapToPrimitiveOrReference<TypeBridge>(
        eachInlinedClass = { inlinedClass, _ ->
            when (inlinedClass.classId) {
                UnsignedType.UBYTE.classId -> return ValueTypeBridge(ObjCValueType.UNSIGNED_CHAR)
                UnsignedType.USHORT.classId -> return ValueTypeBridge(ObjCValueType.UNSIGNED_SHORT)
                UnsignedType.UINT.classId -> return ValueTypeBridge(ObjCValueType.UNSIGNED_INT)
                UnsignedType.ULONG.classId -> return ValueTypeBridge(ObjCValueType.UNSIGNED_LONG_LONG)
            }
        },
        ifPrimitive = { primitiveType, _ ->
            val objCValueType = when (primitiveType) {
                KonanPrimitiveType.BOOLEAN -> ObjCValueType.BOOL
                KonanPrimitiveType.CHAR -> ObjCValueType.UNICHAR
                KonanPrimitiveType.BYTE -> ObjCValueType.CHAR
                KonanPrimitiveType.SHORT -> ObjCValueType.SHORT
                KonanPrimitiveType.INT -> ObjCValueType.INT
                KonanPrimitiveType.LONG -> ObjCValueType.LONG_LONG
                KonanPrimitiveType.FLOAT -> ObjCValueType.FLOAT
                KonanPrimitiveType.DOUBLE -> ObjCValueType.DOUBLE
                KonanPrimitiveType.NON_NULL_NATIVE_PTR -> ObjCValueType.POINTER
            }
            ValueTypeBridge(objCValueType)
        },
        ifReference = {
            if (kotlinType.isFunctionType) {
                bridgeFunctionType(kotlinType)
            } else {
                ReferenceBridge
            }
        }
)

private fun ObjCExportMapper.bridgeFunctionType(kotlinType: KotlinType): TypeBridge {
    // kotlinType.arguments include return type: <P1, P2, ..., Pn, R>
    val numberOfParameters = kotlinType.arguments.size - 1

    val returnType = kotlinType.getReturnTypeFromFunctionType()
    val returnsVoid = returnType.isUnit() || returnType.isNothing()
    // Note: this is correct because overriding method can't turn this into false
    // neither for a parameter nor for a return type.

    return BlockPointerBridge(numberOfParameters, returnsVoid)
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

        returnType.isUnit() || returnType.isNothing() -> if (convertExceptionsToErrors) {
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

internal enum class NSNumberKind(val mappedKotlinClassId: ClassId?, val objCType: ObjCType) {
    CHAR(PrimitiveType.BYTE, "char"),
    UNSIGNED_CHAR(UnsignedType.UBYTE, "unsigned char"),
    SHORT(PrimitiveType.SHORT, "short"),
    UNSIGNED_SHORT(UnsignedType.USHORT, "unsigned short"),
    INT(PrimitiveType.INT, "int"),
    UNSIGNED_INT(UnsignedType.UINT, "unsigned int"),
    LONG("long"),
    UNSIGNED_LONG("unsigned long"),
    LONG_LONG(PrimitiveType.LONG, "long long"),
    UNSIGNED_LONG_LONG(UnsignedType.ULONG, "unsigned long long"),
    FLOAT(PrimitiveType.FLOAT, "float"),
    DOUBLE(PrimitiveType.DOUBLE, "double"),
    BOOL(PrimitiveType.BOOLEAN, "BOOL"),
    INTEGER("NSInteger"),
    UNSIGNED_INTEGER("NSUInteger")

    ;

    // UNSIGNED_SHORT -> unsignedShort
    private val kindName = this.name.split('_')
            .joinToString("") { it.toLowerCase().capitalize() }.decapitalize()


    val valueSelector = kindName // unsignedShort
    val initSelector = "initWith${kindName.capitalize()}:" // initWithUnsignedShort:
    val factorySelector = "numberWith${kindName.capitalize()}:" // numberWithUnsignedShort:

    constructor(
            mappedKotlinClassId: ClassId?,
            objCPrimitiveTypeName: String
    ) : this(mappedKotlinClassId, ObjCPrimitiveType(objCPrimitiveTypeName))

    constructor(
            primitiveType: PrimitiveType,
            objCPrimitiveTypeName: String
    ) : this(ClassId.topLevel(primitiveType.typeFqName), objCPrimitiveTypeName)

    constructor(
            unsignedType: UnsignedType,
            objCPrimitiveTypeName: String
    ) : this(unsignedType.classId, objCPrimitiveTypeName)

    constructor(objCPrimitiveTypeName: String) : this(null, ObjCPrimitiveType(objCPrimitiveTypeName))
}
