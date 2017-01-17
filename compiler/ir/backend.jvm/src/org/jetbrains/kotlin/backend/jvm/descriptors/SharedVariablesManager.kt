/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.common.descriptors.SharedVariablesManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*

class JvmSharedVariablesManager(val builtIns: KotlinBuiltIns) : SharedVariablesManager {
    private val kotlinJvmInternalPackage = KnownPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.jvm.internal"))
    private val refNamespaceClass = KnownClassDescriptor.createClass(Name.identifier("Ref"), kotlinJvmInternalPackage, listOf(builtIns.anyType))

    private class PrimitiveRefDescriptorsProvider(type: KotlinType, refClass: ClassDescriptor) {
        val refType: KotlinType = refClass.defaultType

        val refConstructor: ClassConstructorDescriptor =
                ClassConstructorDescriptorImpl.create(refClass, Annotations.EMPTY, true, SourceElement.NO_SOURCE).apply {
                    initialize(emptyList(), Visibilities.PUBLIC, emptyList())
                    returnType = refType
                }

        val elementField: PropertyDescriptor =
                PropertyDescriptorImpl.create(
                        refClass, Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC, true,
                        Name.identifier("element"), CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE,
                        /* lateInit = */ false, /* isConst = */ false, /* isHeader = */ false, /* isImpl = */ false,
                        /* isExternal = */ false, /* isDelegated = */ false
                ).initialize(type, dispatchReceiverParameter = refClass.thisAsReceiverParameter)
    }

    private val primitiveRefDescriptorProviders: Map<PrimitiveType, PrimitiveRefDescriptorsProvider> =
            PrimitiveType.values().associate {
                val type = builtIns.getPrimitiveKotlinType(it)

                val refClassName = Name.identifier(it.typeName.asString() + "Ref")
                val refClass = KnownClassDescriptor.createClass(refClassName, refNamespaceClass, listOf(builtIns.anyType))

                it to PrimitiveRefDescriptorsProvider(type, refClass)
            }

    private inner class ObjectRefDescriptorsProvider {
        val genericRefClass: ClassDescriptor =
                KnownClassDescriptor.createClassWithTypeParameters(
                        Name.identifier("ObjectRef"), refNamespaceClass, listOf(builtIns.anyType), listOf(Name.identifier("T"))
                )

        val genericRefConstructor: ClassConstructorDescriptor =
                ClassConstructorDescriptorImpl.create(genericRefClass, Annotations.EMPTY, true, SourceElement.NO_SOURCE).apply {
                    initialize(emptyList(), Visibilities.PUBLIC)
                    val typeParameter = typeParameters[0]
                    val typeParameterType = KotlinTypeFactory.simpleType(Annotations.EMPTY, typeParameter.typeConstructor, listOf(), false, MemberScope.Empty)
                    returnType = KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, genericRefClass, listOf(TypeProjectionImpl(Variance.INVARIANT, typeParameterType)))
                }

        val constructorTypeParameter: TypeParameterDescriptor =
                genericRefConstructor.typeParameters[0]

        fun getSubstitutedRefConstructor(valueType: KotlinType): ClassConstructorDescriptor =
                genericRefConstructor.substitute(TypeSubstitutor.create(
                        mapOf(constructorTypeParameter.typeConstructor to TypeProjectionImpl(Variance.INVARIANT, valueType))
                ))!!

        val genericElementField: PropertyDescriptor =
                PropertyDescriptorImpl.create(
                        genericRefClass, Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC, true,
                        Name.identifier("element"), CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE,
                        /* lateInit = */ false, /* isConst = */ false, /* isHeader = */ false, /* isImpl = */ false,
                        /* isExternal = */ false, /* isDelegated = */ false
                ).initialize(
                        type = builtIns.anyType,
                        dispatchReceiverParameter = genericRefClass.thisAsReceiverParameter
                )

        fun getRefType(valueType: KotlinType) =
                KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, genericRefClass, listOf(TypeProjectionImpl(Variance.INVARIANT, valueType)))
    }

    private val objectRefDescriptorsProvider = ObjectRefDescriptorsProvider()

    override fun createSharedVariableDescriptor(variableDescriptor: VariableDescriptor): VariableDescriptor =
            LocalVariableDescriptor(
                    variableDescriptor.containingDeclaration, variableDescriptor.annotations, variableDescriptor.name,
                    getSharedVariableType(variableDescriptor.type),
                    false, false, variableDescriptor.source
            )

    override fun defineSharedValue(sharedVariableDescriptor: VariableDescriptor, originalDeclaration: IrVariable): IrStatement {
        val valueType = originalDeclaration.descriptor.type
        val primitiveRefDescriptorsProvider = primitiveRefDescriptorProviders[getPrimitiveType(valueType)]

        val refConstructor =
                primitiveRefDescriptorsProvider?.refConstructor ?:
                objectRefDescriptorsProvider.getSubstitutedRefConstructor(valueType)

        val refConstructorTypeArguments =
                if (primitiveRefDescriptorsProvider != null) null
                else mapOf(objectRefDescriptorsProvider.constructorTypeParameter to valueType)

        val elementPropertyDescriptor =
                primitiveRefDescriptorsProvider?.elementField ?:
                objectRefDescriptorsProvider.genericElementField

        val refConstructorCall = IrCallImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset,
                refConstructor, refConstructorTypeArguments
        )
        val sharedVariableDeclaration = IrVariableImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset, originalDeclaration.origin,
                sharedVariableDescriptor, refConstructorCall
        )

        val initializer = originalDeclaration.initializer ?:
                          return sharedVariableDeclaration

        val sharedVariableInitialization = IrSetFieldImpl(
                initializer.startOffset, initializer.endOffset,
                elementPropertyDescriptor,
                IrGetValueImpl(initializer.startOffset, initializer.endOffset, sharedVariableDescriptor),
                initializer
        )

        return IrCompositeImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset, builtIns.unitType, null,
                listOf(sharedVariableDeclaration, sharedVariableInitialization)
        )
    }

    private fun getElementFieldDescriptor(valueType: KotlinType): PropertyDescriptor {
        val primitiveRefDescriptorsProvider = primitiveRefDescriptorProviders[getPrimitiveType(valueType)]

        return primitiveRefDescriptorsProvider?.elementField ?:
               objectRefDescriptorsProvider.genericElementField
    }

    override fun getSharedValue(sharedVariableDescriptor: VariableDescriptor, originalGet: IrGetValue): IrExpression =
            IrGetFieldImpl(
                    originalGet.startOffset, originalGet.endOffset,
                    getElementFieldDescriptor(originalGet.descriptor.type),
                    IrGetValueImpl(originalGet.startOffset, originalGet.endOffset, sharedVariableDescriptor),
                    originalGet.origin
            )

    override fun setSharedValue(sharedVariableDescriptor: VariableDescriptor, originalSet: IrSetVariable): IrExpression =
            IrSetFieldImpl(
                    originalSet.startOffset, originalSet.endOffset,
                    getElementFieldDescriptor(originalSet.descriptor.type),
                    IrGetValueImpl(originalSet.startOffset, originalSet.endOffset, sharedVariableDescriptor),
                    originalSet.value,
                    originalSet.origin
            )

    private fun getSharedVariableType(valueType: KotlinType): KotlinType =
            primitiveRefDescriptorProviders[getPrimitiveType(valueType)]?.refType ?:
            objectRefDescriptorsProvider.getRefType(valueType)
    
    private fun getPrimitiveType(type: KotlinType): PrimitiveType? =
            when {
                KotlinBuiltIns.isBoolean(type) -> PrimitiveType.BOOLEAN
                KotlinBuiltIns.isChar(type) -> PrimitiveType.CHAR
                KotlinBuiltIns.isByte(type) -> PrimitiveType.BYTE
                KotlinBuiltIns.isShort(type) -> PrimitiveType.SHORT
                KotlinBuiltIns.isInt(type) -> PrimitiveType.INT
                KotlinBuiltIns.isLong(type) -> PrimitiveType.LONG
                KotlinBuiltIns.isFloat(type) -> PrimitiveType.FLOAT
                KotlinBuiltIns.isDouble(type) -> PrimitiveType.DOUBLE
                else -> null
            }

}
