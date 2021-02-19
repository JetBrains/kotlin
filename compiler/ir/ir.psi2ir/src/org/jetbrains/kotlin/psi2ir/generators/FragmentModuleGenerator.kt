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

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.codegen.CodeFragmentCodegenInfo
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.psi.KtBlockCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile

class FragmentModuleGenerator(
    override val context: GeneratorContext
) : Generator {

    fun generateEvaluatorModuleFragment(
        ktFile: KtBlockCodeFragment,
        codegenInfo: CodeFragmentCodegenInfo
    ): IrModuleFragment =
        IrModuleFragmentImpl(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            val irDeclarationGenerator = FragmentDeclarationGenerator(context)
            irModule.files.add(
                generateSingleFile(irDeclarationGenerator, ktFile, codegenInfo)
            )
        }

    private fun generateSingleFile(
        irDeclarationGenerator: FragmentDeclarationGenerator,
        ktFile: KtBlockCodeFragment,
        codegenInfo: CodeFragmentCodegenInfo
    ): IrFile =
        createEmptyIrFile(ktFile).apply {
            declarations.add(
                irDeclarationGenerator.generateClassForCodeFragment(ktFile, codegenInfo)
            )
            patchDeclarationParents()
        }


    fun generateUnboundSymbolsAsDependencies(irProviders: List<IrProvider>) {
        ExternalDependenciesGenerator(context.symbolTable, irProviders).generateUnboundSymbolsAsDependencies()
    }

    private fun createEmptyIrFile(ktFile: KtFile): IrFileImpl {
        val fileEntry = context.sourceManager.getOrCreateFileEntry(ktFile)
        val packageFragmentDescriptor = context.moduleDescriptor.findPackageFragmentForFile(ktFile)!!
        val irFile = IrFileImpl(fileEntry, packageFragmentDescriptor).apply {
            metadata = DescriptorMetadataSource.File(CodegenUtil.getMemberDescriptorsToGenerate(ktFile, context.bindingContext))
        }
        context.sourceManager.putFileEntry(irFile, fileEntry)
        return irFile
    }
}


//            irModule.files.add(
//                createEmptyIrFile(ktFile).also { irFile ->
//                    val klass = context.symbolTable.declareClass(codegenInfo.classDescriptor) {
//                        context.irFactory.createIrClassFromDescriptor(
//                            UNDEFINED_OFFSET,
//                            UNDEFINED_OFFSET,
//                            IrDeclarationOrigin.DEFINED,
//                            it,
//                            codegenInfo.classDescriptor,
//                            context.symbolTable.nameProvider.nameForDeclaration(codegenInfo.classDescriptor),
//                            codegenInfo.classDescriptor.visibility,
//                            codegenInfo.classDescriptor.modality
//                        ).apply {
//                            thisReceiver = buildReceiverParameter(
//                                this,
//                                IrDeclarationOrigin.INSTANCE_RECEIVER,
//                                symbol.typeWithParameters(typeParameters)
//                            )
//                            val constructor = context.irFactory.createConstructor(
//                                startOffset = UNDEFINED_OFFSET,
//                                endOffset = UNDEFINED_OFFSET,
//                                origin = IrDeclarationOrigin.DEFINED,
//                                symbol = IrConstructorPublicSymbolImpl(context.symbolTable.signaturer.composeSignature(codegenInfo.classDescriptor)!!),
//                                Name.special("<init>"),
//                                codegenInfo.classDescriptor.visibility,
//                                this.defaultType,
//                                isInline = false,
//                                isExternal = false,
//                                isPrimary = true,
//                                isExpect = false
//                            )
//                            constructor.parent = this
//                            constructor.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
//                            addMember(constructor)
//                        }
//                    }
//                    context.symbolTable.declareSimpleFunction(codegenInfo.methodDescriptor) { functionSymbol ->
//                        val function = context.irFactory.createFunction(
//                            UNDEFINED_OFFSET,
//                            UNDEFINED_OFFSET,
//                            IrDeclarationOrigin.DEFINED,
//                            functionSymbol,
//                            context.symbolTable.nameProvider.nameForDeclaration(codegenInfo.methodDescriptor),
//                            codegenInfo.methodDescriptor.visibility,
//                            codegenInfo.methodDescriptor.modality,
//                            codegenInfo.methodDescriptor.returnType?.let { context.typeTranslator.translateType(it) }
//                                ?: context.irBuiltIns.unitType,
//                            isExpect = false,
//                            isExternal = false,
//                            isInfix = false,
//                            isInline = false,
//                            isOperator = false,
//                            isSuspend = false,
//                            isTailrec = false
//                        ).apply {
//                            parent = klass
//                            body = BodyGenerator(
//                                functionSymbol,
//                                context
//                            ).generateExpressionBody(ktFile.getContentElement())
//                        }
//                        klass.addMember(function)
//                        function
//                    }
//                    irFile.declarations.add(klass)
//                    irFile.patchDeclarationParents()
//                }
//            )