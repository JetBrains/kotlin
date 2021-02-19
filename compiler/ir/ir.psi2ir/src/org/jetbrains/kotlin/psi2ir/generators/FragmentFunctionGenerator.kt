/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.codegen.CodeFragmentCodegenInfo
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockCodeFragment
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor

class FragmentFunctionGenerator(declarationGenerator: FragmentDeclarationGenerator) :
    FragmentDeclarationGeneratorExtension(declarationGenerator) {

    fun generateFunctionForFragment(ktFile: KtBlockCodeFragment, codegenInfo: CodeFragmentCodegenInfo): IrSimpleFunction =
        declareFragmentFunction(
            codegenInfo,
            IrDeclarationOrigin.DEFINED,
        ) {
            generateExpressionBody(ktFile.getContentElement())
        }

    private fun declareFragmentFunction(
        codegenInfo: CodeFragmentCodegenInfo,
        origin: IrDeclarationOrigin,
        generateBody: BodyGenerator.() -> IrBody
    ): IrSimpleFunction =
        declareFragmentFunctionInner(codegenInfo.methodDescriptor, origin).buildWithScope { irFunction ->
            generateFragmentFunctionParameterDeclarationsAndReturnType(irFunction)
            irFunction.body = createBodyGenerator(irFunction.symbol).generateBody()
        }

    private fun declareFragmentFunctionInner(descriptor: FunctionDescriptor, origin: IrDeclarationOrigin): IrSimpleFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin,
            descriptor
        )

    private fun generateFragmentFunctionParameterDeclarationsAndReturnType(
        irFunction: IrSimpleFunction
    ) {
        declarationGenerator.generateScopedTypeParameterDeclarations(irFunction, irFunction.descriptor.propertyIfAccessor.typeParameters)
        irFunction.returnType = irFunction.descriptor.returnType!!.toIrType()
        generateFragmentValueParameterDeclarations(irFunction)
    }

    private fun generateFragmentValueParameterDeclarations(
        irFunction: IrSimpleFunction
    ) {
        val functionDescriptor = irFunction.descriptor
        irFunction.valueParameters += functionDescriptor.valueParameters.map { valueParameterDescriptor ->
            declareParameter(valueParameterDescriptor)
        }
    }

    private fun declareParameter(descriptor: ValueParameterDescriptor, name: Name? = null) =
        context.symbolTable.declareValueParameter(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            descriptor, descriptor.type.toIrType(),
            (descriptor as? ValueParameterDescriptor)?.varargElementType?.toIrType(),
            name
        )

}
