/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.codegen.CodeFragmentCodegenInfo
import org.jetbrains.kotlin.ir.PsiIrFileEntry
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
    override val context: GeneratorContext,
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
        val fileEntry = PsiIrFileEntry(ktFile)
        val packageFragmentDescriptor = context.moduleDescriptor.findPackageFragmentForFile(ktFile)!!
        return IrFileImpl(fileEntry, packageFragmentDescriptor).apply {
            metadata = DescriptorMetadataSource.File(CodegenUtil.getMemberDescriptorsToGenerate(ktFile, context.bindingContext))
        }
    }
}