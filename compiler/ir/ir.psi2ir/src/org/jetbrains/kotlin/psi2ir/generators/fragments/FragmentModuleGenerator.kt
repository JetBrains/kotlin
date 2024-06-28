/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators.fragments

import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.psi.KtBlockCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile

class FragmentModuleGenerator(
    override val context: GeneratorContext,
    private val fragmentInfo: EvaluatorFragmentInfo
) : ModuleGenerator(context) {

    override fun generateModuleFragment(
        ktFiles: Collection<KtFile>,
    ): IrModuleFragment {
        assert(ktFiles.singleOrNull { it is KtBlockCodeFragment} != null) {
            "Amongst all files passed to the FragmentModuleGenerator should be exactly one KtBlockCodeFragment"
        }
        return IrModuleFragmentImpl(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            val irDeclarationGenerator = FragmentDeclarationGenerator(context, fragmentInfo)
            ktFiles.forEach { ktFile ->
                irModule.files.add(
                    if (ktFile is KtBlockCodeFragment) {
                        createEmptyIrFile(ktFile, irModule).apply {
                            declarations.add(
                                irDeclarationGenerator.generateClassForCodeFragment(ktFile)
                            )
                            patchDeclarationParents()
                        }
                    } else {
                        generateInContextWithoutFragmentInfo(ktFile) {
                            generateSingleFile(DeclarationGenerator(it), ktFile, irModule)
                        }
                    }
                )
            }
        }
    }

    private fun <T> generateInContextWithoutFragmentInfo(ktFile: KtFile, block: (GeneratorContext) -> T): T {
        val symbolTableDecorator = context.symbolTable as FragmentCompilerSymbolTableDecorator
        val fragmentInfo = symbolTableDecorator.fragmentInfo
        symbolTableDecorator.fragmentInfo = null

        val fileContext = context.createFileScopeContext(ktFile)
        fileContext.fragmentContext = null

        return block(fileContext).also {
            symbolTableDecorator.fragmentInfo = fragmentInfo
            context.additionalDescriptorStorage.addAllFrom(fileContext.additionalDescriptorStorage)
        }
    }

    private fun createEmptyIrFile(ktFile: KtFile, irModule: IrModuleFragment): IrFileImpl {
        val fileEntry = PsiIrFileEntry(ktFile)
        val packageFragmentDescriptor = context.moduleDescriptor.findPackageFragmentForFile(ktFile)!!
        return IrFileImpl(fileEntry, packageFragmentDescriptor, irModule)
    }
}