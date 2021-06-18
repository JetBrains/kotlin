/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.codegen.CodeFragmentCodegenInfo
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockCodeFragment

class FragmentFunctionGenerator(declarationGenerator: FragmentDeclarationGenerator, val codegenInfo: CodeFragmentCodegenInfo) :
    FragmentDeclarationGeneratorExtension(declarationGenerator) {

    fun generateFunctionForFragment(ktFile: KtBlockCodeFragment): IrSimpleFunction =
        declareFragmentFunction(
            IrDeclarationOrigin.DEFINED,
        ) {
            generateExpressionBody(ktFile.getContentElement())
        }

    private fun declareFragmentFunction(
        origin: IrDeclarationOrigin,
        generateBody: BodyGenerator.() -> IrBody
    ): IrSimpleFunction =
        declareFragmentFunctionInner(codegenInfo.methodDescriptor, origin).buildWithScope { irFunction ->
            generateFragmentFunctionParameterDeclarationsAndReturnType(irFunction)
            irFunction.body = createBodyGenerator(irFunction.symbol).generateBody()
        }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun declareFragmentFunctionInner(descriptor: FunctionDescriptor, origin: IrDeclarationOrigin): IrSimpleFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin,
            descriptor
        )

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun generateFragmentFunctionParameterDeclarationsAndReturnType(
        irFunction: IrSimpleFunction
    ) {
        declarationGenerator.generateScopedTypeParameterDeclarations(irFunction)
        irFunction.returnType = irFunction.descriptor.returnType!!.toIrType()
        generateFragmentValueParameterDeclarations(irFunction)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun generateFragmentValueParameterDeclarations(
        irFunction: IrSimpleFunction
    ) {
        val functionDescriptor = irFunction.descriptor
        functionDescriptor.valueParameters.forEachIndexed { index, valueParameterDescriptor ->
            irFunction.valueParameters += declareParameter(valueParameterDescriptor).apply {
                context.capturedDescriptorToFragmentParameterMap[codegenInfo.parameters[index].targetDescriptor] = this.symbol
            }
        }
    }

    private fun declareParameter(descriptor: ValueParameterDescriptor, name: Name? = null) =
        context.symbolTable.declareValueParameter(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            descriptor, descriptor.type.toIrType(),
            (descriptor as? ValueParameterDescriptor)?.varargElementType?.toIrType(),
            name,
            isAssignable = true
        )


}
