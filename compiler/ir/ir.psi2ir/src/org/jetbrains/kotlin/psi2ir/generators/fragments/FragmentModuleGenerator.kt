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
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.ModuleGenerator
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile

class FragmentModuleGenerator(
    override val context: GeneratorContext,
    private val fragmentInfo: EvaluatorFragmentInfo
) : ModuleGenerator(context, expectDescriptorToSymbol = null) {

    override fun generateModuleFragment(
        ktFiles: Collection<KtFile>,
    ): IrModuleFragment {
        val ktBlockCodeFragment = ktFiles.singleOrNull() as? KtBlockCodeFragment
            ?: TODO("Multiple fragments in one compilation not understood and implemented yet")
        return IrModuleFragmentImpl(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            val irDeclarationGenerator = FragmentDeclarationGenerator(context, fragmentInfo)
            irModule.files.add(
                createEmptyIrFile(ktBlockCodeFragment).apply {
                    declarations.add(
                        irDeclarationGenerator.generateClassForCodeFragment(ktBlockCodeFragment)
                    )
                    patchDeclarationParents()
                }
            )
        }
    }

    private fun createEmptyIrFile(ktFile: KtFile): IrFileImpl {
        val fileEntry = PsiIrFileEntry(ktFile)
        val packageFragmentDescriptor = context.moduleDescriptor.findPackageFragmentForFile(ktFile)!!
        return IrFileImpl(fileEntry, packageFragmentDescriptor)
    }
}