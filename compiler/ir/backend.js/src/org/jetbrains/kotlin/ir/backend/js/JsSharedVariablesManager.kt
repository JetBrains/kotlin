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
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.backend.js.utils.createValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance

class JsSharedVariablesManager(val builtIns: KotlinBuiltIns, val jsInterinalPackage: PackageFragmentDescriptor) : SharedVariablesManager {

    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val variableDescriptor = originalDeclaration.descriptor
        val sharedVariableDescriptor = LocalVariableDescriptor(
            variableDescriptor.containingDeclaration, variableDescriptor.annotations, variableDescriptor.name,
            getSharedVariableType(variableDescriptor.type),
            false, false, variableDescriptor.isLateInit, variableDescriptor.source
        )

        val valueType = originalDeclaration.type
        val boxConstructor = closureBoxConstructorTypeDescriptor
        val boxConstructorSymbol = closureBoxConstructorTypeSymbol
        val initializer = originalDeclaration.initializer ?: IrConstImpl.constNull(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            valueType
        )
        // TODO use buildCall?
        val constructorCall = IrCallImpl(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            // TODO wrong type
            originalDeclaration.type,
            boxConstructorSymbol,
            boxConstructor,
            1,
            JsLoweredDeclarationOrigin.JS_CLOSURE_BOX_CLASS
        ).apply {
            putTypeArgument(0, valueType)
            putValueArgument(0, initializer)
        }


        return IrVariableImpl(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            originalDeclaration.origin,
            sharedVariableDescriptor,
            // TODO wrong type ?
            originalDeclaration.type,
            constructorCall
        )
    }

    override fun defineSharedValue(originalDeclaration: IrVariable, sharedVariableDeclaration: IrVariable) = sharedVariableDeclaration

    override fun getSharedValue(sharedVariableSymbol: IrVariableSymbol, originalGet: IrGetValue): IrExpression =
        IrGetFieldImpl(
            originalGet.startOffset, originalGet.endOffset,
            closureBoxFieldSymbol,
            originalGet.type,
            IrGetValueImpl(
                originalGet.startOffset,
                originalGet.endOffset,
                originalGet.type,
                sharedVariableSymbol
            ),
            originalGet.origin
        )

    override fun setSharedValue(sharedVariableSymbol: IrVariableSymbol, originalSet: IrSetVariable): IrExpression =
        IrSetFieldImpl(
            originalSet.startOffset, originalSet.endOffset,
            closureBoxFieldSymbol,
            IrGetValueImpl(
                originalSet.startOffset,
                originalSet.endOffset,
                originalSet.type,
                sharedVariableSymbol
            ),
            originalSet.value,
            originalSet.type,
            originalSet.origin
        )

    private val boxTypeName = "\$closureBox\$"

    private val closureBoxTypeDescriptor = createClosureBoxClass()
    private val closureBoxConstructorTypeDescriptor = createClosureBoxClassConstructor()
    private val closureBoxFieldDescriptor = createClosureBoxField()

    val closureBoxConstructorTypeSymbol = createFunctionSymbol(closureBoxConstructorTypeDescriptor)
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
            val typeParameter = constructedClass.declaredTypeParameters[0]
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

            val paramDesc = createValueParameter(this, 0, "v", parameterType)

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

        desc.setType(builtIns.anyType, emptyList(), closureBoxTypeDescriptor.thisAsReceiverParameter, null)
        desc.initialize(null, null)

        return desc
    }

    private fun getRefType(valueType: KotlinType) =
        KotlinTypeFactory.simpleNotNullType(
            Annotations.EMPTY,
            closureBoxTypeDescriptor,
            listOf(TypeProjectionImpl(Variance.INVARIANT, valueType))
        )

    private fun getSharedVariableType(type: KotlinType) = getRefType(type)

}
