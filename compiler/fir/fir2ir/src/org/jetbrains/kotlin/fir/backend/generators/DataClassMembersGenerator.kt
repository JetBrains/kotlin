/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.DataClassMembersGenerator
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name

class DataClassMembersGenerator(val components: Fir2IrComponents) {

    // TODO: generateInlineClassMembers

    fun generateDataClassMembers(irClass: IrClass): List<Name> =
        MyDataClassMethodsGenerator(irClass, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER).generate()

    private inner class MyDataClassMethodsGenerator(
        val irClass: IrClass,
        val origin: IrDeclarationOrigin
    ) {
        val properties = irClass.declarations.filterIsInstance<IrProperty>().map { it.descriptor }

        private val irDataClassMembersGenerator = object : DataClassMembersGenerator(
            IrGeneratorContextBase(components.irBuiltIns),
            components.symbolTable,
            irClass,
            origin
        ) {
            override fun declareSimpleFunction(startOffset: Int, endOffset: Int, functionDescriptor: FunctionDescriptor): IrFunction {
                throw IllegalStateException("Not expect to see function declaration.")
            }

            override fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
                // TODO
            }

            override fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression, descriptor: CallableDescriptor) {
                // TODO
            }
        }

        fun generateDispatchReceiverParameter(irFunction: IrFunction, valueParameterDescriptor: WrappedValueParameterDescriptor) =
            irFunction.declareThisReceiverParameter(
                components.symbolTable,
                irClass.defaultType,
                origin,
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET
            ).apply {
                valueParameterDescriptor.bind(this)
            }

        fun generate(): List<Name> {
            if (properties.isEmpty()) {
                return emptyList()
            }

            // TODO: consolidate componentN() and copy(...) too?

            // TODO: generate equals, hashCode, and toString only if needed
            val equalsDescriptor = WrappedSimpleFunctionDescriptor()
            val equalsDispatchReceiverDescriptor = WrappedValueParameterDescriptor()
            val equalsValueParameterDescriptor = WrappedValueParameterDescriptor()
            val equalsFunction =
                components.symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, equalsDescriptor) { symbol ->
                    IrFunctionImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        origin,
                        symbol,
                        Name.identifier("equals"),
                        Visibilities.PUBLIC,
                        Modality.OPEN,
                        components.irBuiltIns.booleanType,
                        isInline = false,
                        isExternal = false,
                        isTailrec = false,
                        isSuspend = false,
                        isExpect = false,
                        isFakeOverride = false,
                        isOperator = false
                    ).apply {
                        metadata = MetadataSource.Function(descriptor)
                    }
                }.apply {
                    parent = irClass
                    equalsDescriptor.bind(this)
                    dispatchReceiverParameter = generateDispatchReceiverParameter(this, equalsDispatchReceiverDescriptor)
                    val irFunction = this
                    valueParameters +=
                        components.symbolTable.declareValueParameter(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            origin,
                            equalsValueParameterDescriptor,
                            components.irBuiltIns.anyNType
                        ) { symbol ->
                            IrValueParameterImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                origin,
                                symbol,
                                Name.identifier("other"),
                                0,
                                components.irBuiltIns.anyNType,
                                null,
                                isCrossinline = false,
                                isNoinline = false
                            )
                        }.apply {
                            parent = irFunction
                            equalsValueParameterDescriptor.bind(this)
                        }
                }
            irDataClassMembersGenerator.generateEqualsMethod(equalsFunction, properties)

            val hashCodeDescriptor = WrappedSimpleFunctionDescriptor()
            val hashCodeDispatchReceiverDescriptor = WrappedValueParameterDescriptor()
            val hashCodeFunction =
                components.symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, hashCodeDescriptor) { symbol ->
                    IrFunctionImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        origin,
                        symbol,
                        Name.identifier("hashCode"),
                        Visibilities.PUBLIC,
                        Modality.OPEN,
                        components.irBuiltIns.intType,
                        isInline = false,
                        isExternal = false,
                        isTailrec = false,
                        isSuspend = false,
                        isExpect = false,
                        isFakeOverride = false,
                        isOperator = false
                    ).apply {
                        metadata = MetadataSource.Function(descriptor)
                    }
                }.apply {
                    parent = irClass
                    hashCodeDescriptor.bind(this)
                    dispatchReceiverParameter = generateDispatchReceiverParameter(this, hashCodeDispatchReceiverDescriptor)
                }
            irDataClassMembersGenerator.generateHashCodeMethod(hashCodeFunction, properties)

            val toStringDescriptor = WrappedSimpleFunctionDescriptor()
            val toStringDispatchReceiverDescriptor = WrappedValueParameterDescriptor()
            val toStringFunction =
                components.symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, toStringDescriptor) { symbol ->
                    IrFunctionImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        origin,
                        symbol,
                        Name.identifier("toString"),
                        Visibilities.PUBLIC,
                        Modality.OPEN,
                        components.irBuiltIns.stringType,
                        isInline = false,
                        isExternal = false,
                        isTailrec = false,
                        isSuspend = false,
                        isExpect = false,
                        isFakeOverride = false,
                        isOperator = false
                    ).apply {
                        metadata = MetadataSource.Function(descriptor)
                    }
                }.apply {
                    parent = irClass
                    toStringDescriptor.bind(this)
                    dispatchReceiverParameter = generateDispatchReceiverParameter(this, toStringDispatchReceiverDescriptor)
                }
            irDataClassMembersGenerator.generateToStringMethod(toStringFunction, properties)

            return listOf(equalsFunction.name, hashCodeFunction.name, toStringFunction.name)
        }
    }
}