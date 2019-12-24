/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.factories.createFile
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile
import org.jetbrains.kotlin.utils.addIfNotNull

class ModuleGenerator(override val context: GeneratorContext) : Generator {

    private val constantValueGenerator = context.constantValueGenerator

    fun generateModuleFragment(ktFiles: Collection<KtFile>, deserializer: IrDeserializer, extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY): IrModuleFragment =
        generateModuleFragmentWithoutDependencies(ktFiles).also { irModule ->
            generateUnboundSymbolsAsDependencies(irModule, deserializer, extensions)
        }

    fun generateModuleFragmentWithoutDependencies(ktFiles: Collection<KtFile>): IrModuleFragment =
        context.irDeclarationFactory.createModuleFragment(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            irModule.files.addAll(generateFiles(ktFiles))
        }

    fun generateUnboundSymbolsAsDependencies(
        irModule: IrModuleFragment,
        deserializer: IrDeserializer? = null,
        extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY
    ) {
        val fullIrProvidersList = generateTypicalIrProviderList(
            irModule.descriptor, context.irBuiltIns, context.symbolTable, deserializer,
            extensions
        )
        ExternalDependenciesGenerator(context.symbolTable, fullIrProvidersList).generateUnboundSymbolsAsDependencies()
    }

    fun generateUnboundSymbolsAsDependencies(irProviders: List<IrProvider>) {
        ExternalDependenciesGenerator(context.symbolTable, irProviders).generateUnboundSymbolsAsDependencies()
    }

    private fun generateFiles(ktFiles: Collection<KtFile>): List<IrFile> {
        val irDeclarationGenerator = DeclarationGenerator(context)

        return ktFiles.map { ktFile ->
            generateSingleFile(irDeclarationGenerator, ktFile)
        }
    }

    private fun generateSingleFile(irDeclarationGenerator: DeclarationGenerator, ktFile: KtFile): IrFile {
        val irFile = createEmptyIrFile(ktFile)

        for (ktAnnotationEntry in ktFile.annotationEntries) {
            val annotationDescriptor = getOrFail(BindingContext.ANNOTATION, ktAnnotationEntry)
            irFile.annotations.addIfNotNull(constantValueGenerator.generateAnnotationConstructorCall(annotationDescriptor))
        }

        for (ktDeclaration in ktFile.declarations) {
            irFile.declarations.addIfNotNull(irDeclarationGenerator.generateMemberDeclaration(ktDeclaration))
        }

        return irFile
    }

    private fun createEmptyIrFile(ktFile: KtFile): IrFile {
        val fileEntry = context.sourceManager.getOrCreateFileEntry(ktFile)
        val packageFragmentDescriptor = context.moduleDescriptor.findPackageFragmentForFile(ktFile)!!
        val irFile = context.irDeclarationFactory.createFile(fileEntry, packageFragmentDescriptor).apply {
            metadata = MetadataSource.File(CodegenUtil.getMemberDescriptorsToGenerate(ktFile, context.bindingContext))
        }
        context.sourceManager.putFileEntry(irFile, fileEntry)
        return irFile
    }
}
