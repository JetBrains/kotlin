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

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile

class ModuleGenerator(override val context: GeneratorContext) : Generator {

    private val constantValueGenerator = context.constantValueGenerator

    fun generateModuleFragment(ktFiles: Collection<KtFile>): IrModuleFragment =
        generateModuleFragmentWithoutDependencies(ktFiles).also { irModule ->
            generateUnboundSymbolsAsDependencies(irModule)
        }

    fun generateModuleFragmentWithoutDependencies(ktFiles: Collection<KtFile>): IrModuleFragment =
        IrModuleFragmentImpl(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            irModule.files.addAll(generateFiles(ktFiles))
        }

    fun generateUnboundSymbolsAsDependencies(irModule: IrModuleFragment) {
        ExternalDependenciesGenerator(
            irModule.descriptor, context.symbolTable, context.irBuiltIns
        ).generateUnboundSymbolsAsDependencies(irModule, context.bindingContext)
    }

    private fun generateFiles(ktFiles: Collection<KtFile>): List<IrFile> {
        val irDeclarationGenerator = DeclarationGenerator(context)

        return ktFiles.map { ktFile ->
            generateSingleFile(irDeclarationGenerator, ktFile)
        }
    }

    private fun generateSingleFile(irDeclarationGenerator: DeclarationGenerator, ktFile: KtFile): IrFileImpl {
        val irFile = createEmptyIrFile(ktFile)

        for (ktAnnotationEntry in ktFile.annotationEntries) {
            val annotationDescriptor = getOrFail(BindingContext.ANNOTATION, ktAnnotationEntry)
            irFile.fileAnnotations.add(annotationDescriptor)
            irFile.annotations.add(constantValueGenerator.generateAnnotationConstructorCall(annotationDescriptor))
        }

        for (ktDeclaration in ktFile.declarations) {
            irFile.declarations.add(irDeclarationGenerator.generateMemberDeclaration(ktDeclaration))
        }

        return irFile
    }

    private fun createEmptyIrFile(ktFile: KtFile): IrFileImpl {
        val fileEntry = context.sourceManager.getOrCreateFileEntry(ktFile)
        val packageFragmentDescriptor = context.moduleDescriptor.findPackageFragmentForFile(ktFile)!!
        val irFile = IrFileImpl(fileEntry, packageFragmentDescriptor)
        context.sourceManager.putFileEntry(irFile, fileEntry)
        return irFile
    }
}
