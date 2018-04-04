/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.descriptors.KnownClassDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.SharedVariablesManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*

class JsSharedVariablesManager(val builtIns: KotlinBuiltIns, val jsInterinalPackage: PackageFragmentDescriptor) : SharedVariablesManager {

    override fun createSharedVariableDescriptor(variableDescriptor: VariableDescriptor): VariableDescriptor =
        LocalVariableDescriptor(
            variableDescriptor.containingDeclaration, Annotations.EMPTY, variableDescriptor.name,
            getSharedVariableType(variableDescriptor.type),
            false, false, variableDescriptor.isLateInit, variableDescriptor.source
        )

    override fun defineSharedValue(sharedVariableDescriptor: VariableDescriptor, originalDeclaration: IrVariable): IrStatement {
        val valueType = originalDeclaration.descriptor.type
        val boxConstructor = closureBoxConstructorTypeDescriptor
        val boxConstructorSymbol = closureBoxConstrctorTypeSymbol
        val constructorTypeParam = closureBoxConstructorTypeDescriptor.typeParameters[0]
        val boxConstructorTypeArgument = mapOf(constructorTypeParam to valueType)
        val initializer = originalDeclaration.initializer ?: IrConstImpl.constNull(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            valueType
        )
        val constructorCall = IrCallImpl(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            boxConstructorSymbol,
            boxConstructor,
            boxConstructorTypeArgument,
            JsLoweredDeclarationOrigin.JS_CLOSURE_BOX_CLASS
        ).apply { putValueArgument(0, initializer) }


        return IrVariableImpl(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            originalDeclaration.origin,
            sharedVariableDescriptor,
            constructorCall
        )
    }

    override fun getSharedValue(sharedVariableDescriptor: VariableDescriptor, originalGet: IrGetValue): IrExpression =
        IrGetFieldImpl(
            originalGet.startOffset, originalGet.endOffset,
            closureBoxFieldSymbol,
            IrGetValueImpl(
                originalGet.startOffset,
                originalGet.endOffset,
                symbolForDescriptor(sharedVariableDescriptor)
            ),
            originalGet.origin
        )

    override fun setSharedValue(sharedVariableDescriptor: VariableDescriptor, originalSet: IrSetVariable): IrExpression =
        IrSetFieldImpl(
            originalSet.startOffset, originalSet.endOffset,
            closureBoxFieldSymbol,
            IrGetValueImpl(
                originalSet.startOffset,
                originalSet.endOffset,
                symbolForDescriptor(sharedVariableDescriptor)
            ),
            originalSet.value,
            originalSet.origin
        )

    private val boxTypeName = "\$closureBox\$"

    private val closureBoxTypeDescriptor = createClosureBoxClass()
    private val closureBoxConstructorTypeDescriptor = createClosureBoxClassConstructor()
    private val closureBoxFieldDescriptor = createClosureBoxField()

    private val closureBoxTypeSymbol = IrClassSymbolImpl(closureBoxTypeDescriptor)
    val closureBoxConstrctorTypeSymbol = createFunctionSymbol(closureBoxConstructorTypeDescriptor)
    private val closureBoxFieldSymbol = IrFieldSymbolImpl(closureBoxFieldDescriptor)


    private fun createClosureBoxClass(): ClassDescriptor =
        KnownClassDescriptor.createClassWithTypeParameters(
            Name.identifier(boxTypeName), jsInterinalPackage, listOf(builtIns.anyType), listOf(
                Name.identifier("T")
            )
        )

    private fun createClosureBoxClassConstructor(): ClassConstructorDescriptor =
        ClassConstructorDescriptorImpl.create(
            closureBoxTypeDescriptor,
            Annotations.EMPTY,
            true,
            SourceElement.NO_SOURCE
        ).apply {

            /// constructor<T>(v: T) : T

            val typeParameter = constructedClass.declaredTypeParameters[0]


//            val typeParameter = typeParameters[0]
            val typeParameterType = KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
                Annotations.EMPTY,
                typeParameter.typeConstructor,
                listOf(),
                false,
                MemberScope.Empty
            )

            val parameterType = KotlinTypeFactory.simpleType(
                Annotations.EMPTY,
                typeParameter.typeConstructor,
                listOf(), true
            )

            val paramDesc = ValueParameterDescriptorImpl(
                this,
                null,
                0,
                Annotations.EMPTY,
                Name.identifier("v"),
                parameterType,
                false,
                false,
                false,
                null,
                SourceElement.NO_SOURCE
            )

            initialize(listOf(paramDesc), Visibilities.PUBLIC)
            returnType = KotlinTypeFactory.simpleNotNullType(
                Annotations.EMPTY,
                closureBoxTypeDescriptor,
                listOf(TypeProjectionImpl(Variance.INVARIANT, typeParameterType))
            )
        }

    private fun createClosureBoxField(): PropertyDescriptor {
        val desc = PropertyDescriptorImpl.create(
            closureBoxTypeDescriptor,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PUBLIC,
            true,
            Name.identifier("v"),
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false,
            false,
            false,
            false,
            false,
            false
        )


        desc.setType(builtIns.anyType, emptyList(), closureBoxTypeDescriptor.thisAsReceiverParameter, null as ReceiverParameterDescriptor?)
        desc.initialize(null, null)

        return desc
    }

    private fun getSubstitutedRefConstructor(valueType: KotlinType): ClassConstructorDescriptor =
        closureBoxConstructorTypeDescriptor.substitute(
            TypeSubstitutor.create(
                closureBoxConstructorTypeDescriptor.typeParameters.associate {
                    it.typeConstructor to TypeProjectionImpl(
                        Variance.INVARIANT,
                        valueType
                    )
                }
            )
        )!!

    private fun getRefType(valueType: KotlinType) =
        KotlinTypeFactory.simpleNotNullType(
            Annotations.EMPTY,
            closureBoxTypeDescriptor,
            listOf(TypeProjectionImpl(Variance.INVARIANT, valueType))
        )

    private fun getSharedVariableType(type: KotlinType) = getRefType(type)

    private val variableSymbols = mutableMapOf<VariableDescriptor, IrValueSymbol>()

    private fun symbolForDescriptor(sharedVariableDescriptor: VariableDescriptor): IrValueSymbol =
        variableSymbols.getOrPut(sharedVariableDescriptor, { IrVariableSymbolImpl(sharedVariableDescriptor) })
}
