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
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.transformations.insertImplicitCasts
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.utils.addIfNotNull

open class ModuleGenerator(override val context: GeneratorContext) : Generator {

    open fun generateModuleFragment(ktFiles: Collection<KtFile>): IrModuleFragment =
        IrModuleFragmentImpl(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            ktFiles.toSet().mapTo(irModule.files) { ktFile ->
                val fileContext = context.createFileScopeContext(ktFile)
                val irDeclarationGenerator = DeclarationGenerator(fileContext)
                generateSingleFile(irDeclarationGenerator, ktFile, irModule)
            }
        }

    fun generateUnboundSymbolsAsDependencies(irProviders: List<IrProvider>) {
        ExternalDependenciesGenerator(context.symbolTable, irProviders).generateUnboundSymbolsAsDependencies()
    }

    fun generateSingleFile(irDeclarationGenerator: DeclarationGenerator, ktFile: KtFile, module: IrModuleFragment): IrFileImpl {
        val irFile = createEmptyIrFile(ktFile, module)

        val constantValueGenerator = irDeclarationGenerator.context.constantValueGenerator
        for (ktAnnotationEntry in ktFile.annotationEntries) {
            val annotationDescriptor = getOrFail(BindingContext.ANNOTATION, ktAnnotationEntry)
            constantValueGenerator.generateAnnotationConstructorCall(annotationDescriptor)?.let {
                irFile.annotations += it
            }
        }

        for (ktDeclaration in ktFile.declarations) {
            irFile.declarations.addIfNotNull(irDeclarationGenerator.generateMemberDeclaration(ktDeclaration))
        }

        irFile.patchDeclarationParents()

        IrSyntheticDeclarationGenerator(context).generateSyntheticDeclarations(irFile)

        if (context.configuration.skipBodies) {
            // In KAPT3 mode (skipBodies = true), we create stub IR for error class, so that it would be possible to have annotations
            // with unresolved types in the IR and they could survive all transformations up until the codegen.
            // This is a hack which should preferably be removed as soon as KAPT3 is no longer used.
            createStubIrForErrorClass()
        }

        insertImplicitCasts(irFile, context)
        context.callToSubstitutedDescriptorMap.clear()

        irFile.acceptVoid(AnnotationGenerator(context))

        irFile.patchDeclarationParents()

        return irFile
    }

    private fun createEmptyIrFile(ktFile: KtFile, module: IrModuleFragment): IrFileImpl {
        val fileEntry = PsiIrFileEntry(ktFile)
        val packageFragmentDescriptor = context.moduleDescriptor.findPackageFragmentForFile(ktFile)!!
        return IrFileImpl(fileEntry, packageFragmentDescriptor, module).apply {
            metadata = DescriptorMetadataSource.File(CodegenUtil.getMemberDescriptorsToGenerate(ktFile, context.bindingContext))
        }
    }

    private fun createStubIrForErrorClass() {
        val fakeFileEntry = object : IrFileEntry {
            override val name: String = "<error-class>"
            override val maxOffset: Int = UNDEFINED_OFFSET

            override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo = TODO("Not yet implemented")
            override fun getLineNumber(offset: Int): Int = TODO("Not yet implemented")
            override fun getColumnNumber(offset: Int): Int = TODO("Not yet implemented")
            override fun getLineAndColumnNumbers(offset: Int): LineAndColumn = TODO("Not yet implemented")
        }
        val fakeFile = IrFileImpl(
            fakeFileEntry,
            EmptyPackageFragmentDescriptor(context.moduleDescriptor, FqName(fakeFileEntry.name)),
        )
        val gen = SyntheticDeclarationsGenerator(context)
        gen.visitClassDescriptor(ErrorUtils.errorClass, fakeFile)
        gen.visitConstructorDescriptor(
            ErrorUtils.errorClass.unsubstitutedPrimaryConstructor!!,
            context.symbolTable.descriptorExtension.referenceClass(ErrorUtils.errorClass).owner,
        )
    }
}
