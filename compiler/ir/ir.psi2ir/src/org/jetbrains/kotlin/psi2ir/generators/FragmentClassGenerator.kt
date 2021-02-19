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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.codegen.CodeFragmentCodegenInfo
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorPublicSymbolImpl
import org.jetbrains.kotlin.ir.util.createIrClassFromDescriptor
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockCodeFragment

class FragmentClassGenerator(
    declarationGenerator: FragmentDeclarationGenerator
) : FragmentDeclarationGeneratorExtension(declarationGenerator) {

    fun generateClassForCodeFragment(ktFile: KtBlockCodeFragment, codegenInfo: CodeFragmentCodegenInfo): IrClass {
        val classDescriptor = codegenInfo.classDescriptor
        val startOffset = UNDEFINED_OFFSET
        val endOffset = UNDEFINED_OFFSET
        val visibility = classDescriptor.visibility
        val modality = Modality.FINAL

        return context.symbolTable.declareClass(classDescriptor) {
            context.irFactory.createIrClassFromDescriptor(
                startOffset, endOffset, IrDeclarationOrigin.DEFINED, it, classDescriptor,
                context.symbolTable.nameProvider.nameForDeclaration(classDescriptor), visibility, modality
            )
        }.buildWithScope { irClass ->
            irClass.thisReceiver = context.symbolTable.declareValueParameter(
                startOffset, endOffset,
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                classDescriptor.thisAsReceiverParameter,
                classDescriptor.thisAsReceiverParameter.type.toIrType()
            )


            generatePrimaryConstructor(irClass, codegenInfo)

            irClass.declarations.add(
                generateMemberForFragmentClass(ktFile, codegenInfo)
            )
        }
    }


    private fun generatePrimaryConstructor(irClass: IrClass, codegenInfo: CodeFragmentCodegenInfo) {
        val constructor = context.irFactory.createConstructor(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            symbol = IrConstructorPublicSymbolImpl(context.symbolTable.signaturer.composeSignature(codegenInfo.classDescriptor)!!),
            Name.special("<init>"),
            codegenInfo.classDescriptor.visibility,
            irClass.defaultType,
            isInline = false,
            isExternal = false,
            isPrimary = true,
            isExpect = false
        )
        constructor.parent = irClass
        constructor.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        irClass.addMember(constructor)
    }

    private fun generateMemberForFragmentClass(ktFile: KtBlockCodeFragment, codegenInfo: CodeFragmentCodegenInfo): IrSimpleFunction {
        return declarationGenerator.generateFunctionForFragment(ktFile, codegenInfo)
    }

}
