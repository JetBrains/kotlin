package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.jvm.descriptors.initialize
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.createSimpleDelegatingConstructorDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

internal data class LoweredEnum(val implObjectDescriptor: ClassDescriptor,
                                val valuesProperty: PropertyDescriptor,
                                val valuesGetter: FunctionDescriptor,
                                val valuesFunction: FunctionDescriptor,
                                val valueOfFunction: FunctionDescriptor,
                                val itemGetter: FunctionDescriptor,
                                val entriesMap: Map<Name, Int>)

internal class EnumSpecialDescriptorsFactory(val context: Context) {
    fun createLoweredEnum(enumClassDescriptor: ClassDescriptor): LoweredEnum {
        val implObjectDescriptor = ClassDescriptorImpl(enumClassDescriptor, "OBJECT".synthesizedName, Modality.FINAL,
                ClassKind.OBJECT, KonanPlatform.builtIns.anyType.singletonList(), SourceElement.NO_SOURCE, false)

        val valuesProperty = createEnumValuesField(enumClassDescriptor, implObjectDescriptor)
        val valuesFunction = createValuesFunctionDescriptor(enumClassDescriptor, implObjectDescriptor)
        val valueOfFunction = createValueOfFunctionDescriptor(enumClassDescriptor, implObjectDescriptor)
        val valuesGetter = createValuesGetterDescriptor(enumClassDescriptor, implObjectDescriptor)

        val memberScope = MemberScope.Empty

        val constructorOfAny = context.builtIns.any.constructors.first()
        // TODO: why primary?
        val constructorDescriptor = implObjectDescriptor.createSimpleDelegatingConstructorDescriptor(constructorOfAny, true)

        implObjectDescriptor.initialize(memberScope, setOf(constructorDescriptor), constructorDescriptor)

        return LoweredEnum(implObjectDescriptor, valuesProperty, valuesGetter, valuesFunction, valueOfFunction,
                getEnumItemGetter(enumClassDescriptor), createEnumEntriesMap(enumClassDescriptor))
    }

    private fun createValuesFunctionDescriptor(enumClassDescriptor: ClassDescriptor, implObjectDescriptor: ClassDescriptor)
            : FunctionDescriptor {
        val returnType = genericArrayType.defaultType.replace(listOf(TypeProjectionImpl(enumClassDescriptor.defaultType)))
        val result = SimpleFunctionDescriptorImpl.create(
                /* containingDeclaration        = */ implObjectDescriptor,
                /* annotations                  = */ Annotations.EMPTY,
                /* name                         = */ "values".synthesizedName,
                /* kind                         = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                /* source                       = */ SourceElement.NO_SOURCE)
        result.initialize(
                /* receiverParameterType        = */ null,
                /* dispatchReceiverParameter    = */ null,
                /* typeParameters               = */ listOf(),
                /* unsubstitutedValueParameters = */ listOf(),
                /* unsubstitutedReturnType      = */ returnType,
                /* modality                     = */ Modality.FINAL,
                /* visibility                   = */ Visibilities.PUBLIC)
        return result
    }

    private fun createValuesGetterDescriptor(enumClassDescriptor: ClassDescriptor, implObjectDescriptor: ClassDescriptor)
            : FunctionDescriptor {
        val returnType = genericArrayType.defaultType.replace(listOf(TypeProjectionImpl(enumClassDescriptor.defaultType)))
        val result = SimpleFunctionDescriptorImpl.create(
                /* containingDeclaration        = */ implObjectDescriptor,
                /* annotations                  = */ Annotations.EMPTY,
                /* name                         = */ "get-VALUES".synthesizedName,
                /* kind                         = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                /* source                       = */ SourceElement.NO_SOURCE)
        result.initialize(
                /* receiverParameterType        = */ null,
                /* dispatchReceiverParameter    = */ null,
                /* typeParameters               = */ listOf(),
                /* unsubstitutedValueParameters = */ listOf(),
                /* unsubstitutedReturnType      = */ returnType,
                /* modality                     = */ Modality.FINAL,
                /* visibility                   = */ Visibilities.PUBLIC)
        return result
    }

    private fun createValueOfFunctionDescriptor(enumClassDescriptor: ClassDescriptor, implObjectDescriptor: ClassDescriptor)
            : FunctionDescriptor {
        val result = SimpleFunctionDescriptorImpl.create(
                /* containingDeclaration        = */ implObjectDescriptor,
                /* annotations                  = */ Annotations.EMPTY,
                /* name                         = */ "valueOf".synthesizedName,
                /* kind                         = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                /* source                       = */ SourceElement.NO_SOURCE)
        val nameParameterDescriptor = ValueParameterDescriptorImpl(
                containingDeclaration = result,
                original = null,
                index = 0,
                annotations = Annotations.EMPTY,
                name = Name.identifier("name"),
                outType = context.builtIns.stringType,
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                source = SourceElement.NO_SOURCE)
        result.initialize(
                /* receiverParameterType        = */ null,
                /* dispatchReceiverParameter    = */ null,
                /* typeParameters               = */ listOf(),
                /* unsubstitutedValueParameters = */ listOf(nameParameterDescriptor),
                /* unsubstitutedReturnType      = */ enumClassDescriptor.defaultType,
                /* modality                     = */ Modality.FINAL,
                /* visibility                   = */ Visibilities.PUBLIC)
        return result
    }

    private fun createEnumValuesField(enumClassDescriptor: ClassDescriptor, implObjectDescriptor: ClassDescriptor): PropertyDescriptor {
        val valuesArrayType = context.builtIns.getArrayType(Variance.INVARIANT, enumClassDescriptor.defaultType)
        val receiver = ReceiverParameterDescriptorImpl(implObjectDescriptor, ImplicitClassReceiver(implObjectDescriptor))
        return PropertyDescriptorImpl.create(implObjectDescriptor, Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC,
                false, "VALUES".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
                false, false, false, false, false, false).initialize(valuesArrayType, dispatchReceiverParameter = receiver)
    }

    private val kotlinPackage = context.irModule!!.descriptor.getPackage(FqName("kotlin"))
    private val genericArrayType = kotlinPackage.memberScope.getContributedClassifier(Name.identifier("Array"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    private fun getEnumItemGetter(enumClassDescriptor: ClassDescriptor): FunctionDescriptor {
        val getter = genericArrayType.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("get"), NoLookupLocation.FROM_BACKEND).single()

        val typeParameterT = genericArrayType.declaredTypeParameters[0]
        val enumClassType = enumClassDescriptor.defaultType
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
        return getter.substitute(typeSubstitutor)!!
    }

    private fun createEnumEntriesMap(enumClassDescriptor: ClassDescriptor): Map<Name, Int> {
        val map = mutableMapOf<Name, Int>()
        enumClassDescriptor.unsubstitutedMemberScope.getContributedDescriptors()
                .filter { it is ClassDescriptor && it.kind == ClassKind.ENUM_ENTRY }
                .sortedBy { it.name }
                .forEachIndexed { index, entry -> map.put(entry.name, index) }
        return map
    }

}