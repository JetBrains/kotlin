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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.jvm.descriptors.initialize
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.createSimpleDelegatingConstructorDescriptor
import org.jetbrains.kotlin.backend.konan.descriptors.enumEntries
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.replace


internal object DECLARATION_ORIGIN_ENUM :
        IrDeclarationOriginImpl("ENUM")

internal data class LoweredEnum(val implObject: IrClass,
                                val valuesField: IrField,
                                val valuesGetter: IrSimpleFunction,
                                val itemGetterSymbol: IrSimpleFunctionSymbol,
                                val itemGetterDescriptor: FunctionDescriptor,
                                val entriesMap: Map<Name, Int>)

internal class EnumSpecialDeclarationsFactory(val context: Context) {
    fun createLoweredEnum(enumClassDescriptor: ClassDescriptor): LoweredEnum {

        val startOffset = enumClassDescriptor.startOffsetOrUndefined
        val endOffset = enumClassDescriptor.endOffsetOrUndefined

        val implObjectDescriptor = ClassDescriptorImpl(enumClassDescriptor, "OBJECT".synthesizedName, Modality.FINAL,
                ClassKind.OBJECT, listOf(context.builtIns.anyType), SourceElement.NO_SOURCE, false, LockBasedStorageManager.NO_LOCKS)

        val valuesProperty = createEnumValuesField(enumClassDescriptor, implObjectDescriptor)
        val valuesField = IrFieldImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM, valuesProperty)

        val valuesGetterDescriptor = createValuesGetterDescriptor(enumClassDescriptor, implObjectDescriptor)
        val valuesGetter = IrFunctionImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM, valuesGetterDescriptor).apply {
            createParameterDeclarations()
        }

        val memberScope = MemberScope.Empty

        val constructorOfAny = context.builtIns.any.constructors.first()
        // TODO: why primary?
        val constructorDescriptor = implObjectDescriptor.createSimpleDelegatingConstructorDescriptor(constructorOfAny, true)

        implObjectDescriptor.initialize(memberScope, setOf(constructorDescriptor), constructorDescriptor)
        val implObject = IrClassImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM, implObjectDescriptor).apply {
            createParameterDeclarations()
            addFakeOverrides()
            setSuperSymbols(listOf(context.ir.symbols.any.owner))
        }
        implObject.parent = context.ir.getEnum(enumClassDescriptor)
        valuesGetter.parent = implObject
        valuesField.parent = implObject

        val (itemGetterSymbol, itemGetterDescriptor) = getEnumItemGetter(enumClassDescriptor)

        return LoweredEnum(
                implObject,
                valuesField,
                valuesGetter,
                itemGetterSymbol, itemGetterDescriptor,
                createEnumEntriesMap(enumClassDescriptor))
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

    private fun createEnumValuesField(enumClassDescriptor: ClassDescriptor, implObjectDescriptor: ClassDescriptor): PropertyDescriptor {
        val valuesArrayType = context.builtIns.getArrayType(Variance.INVARIANT, enumClassDescriptor.defaultType)
        val receiver = ReceiverParameterDescriptorImpl(implObjectDescriptor, ImplicitClassReceiver(implObjectDescriptor))
        return PropertyDescriptorImpl.create(implObjectDescriptor, Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC,
                false, "VALUES".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
                false, false, false, false, false, false).initialize(valuesArrayType, dispatchReceiverParameter = receiver)
    }

    private val genericArrayType = context.ir.symbols.array.descriptor

    private fun getEnumItemGetter(enumClassDescriptor: ClassDescriptor): Pair<IrSimpleFunctionSymbol, FunctionDescriptor> {
        val getter = context.ir.symbols.array.functions.single { it.descriptor.name == Name.identifier("get") }

        val typeParameterT = genericArrayType.declaredTypeParameters[0]
        val enumClassType = enumClassDescriptor.defaultType
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
        return getter to getter.descriptor.substitute(typeSubstitutor)!!
    }

    private fun createEnumEntriesMap(enumClassDescriptor: ClassDescriptor): Map<Name, Int> {
        val map = mutableMapOf<Name, Int>()
        enumClassDescriptor.enumEntries
                .sortedBy { it.name }
                .forEachIndexed { index, entry -> map.put(entry.name, index) }
        return map
    }

}
