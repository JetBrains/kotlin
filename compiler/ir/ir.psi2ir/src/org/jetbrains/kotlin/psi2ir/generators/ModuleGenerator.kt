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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWithParameters
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.transformations.insertImplicitCasts
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile
import org.jetbrains.kotlin.utils.addIfNotNull

class ModuleGenerator(
    override val context: GeneratorContext,
    private val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null
) : Generator {
    private val constantValueGenerator = context.constantValueGenerator

    fun generateModuleFragment(ktFiles: Collection<KtFile>): IrModuleFragment =
        IrModuleFragmentImpl(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            val irDeclarationGenerator = DeclarationGenerator(context)
            ktFiles.mapTo(irModule.files) { ktFile ->
                generateSingleFile(irDeclarationGenerator, ktFile)
            }
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
        ExternalDependenciesGenerator(context.symbolTable, fullIrProvidersList)
            .generateUnboundSymbolsAsDependencies()
    }

    fun generateUnboundSymbolsAsDependencies(irProviders: List<IrProvider>) {
        ExternalDependenciesGenerator(context.symbolTable, irProviders)
            .generateUnboundSymbolsAsDependencies()
    }

    private fun generateSingleFile(irDeclarationGenerator: DeclarationGenerator, ktFile: KtFile): IrFileImpl {
        val irFile = createEmptyIrFile(ktFile)

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

        if (expectDescriptorToSymbol != null) {
            referenceExpectsForUsedActuals(expectDescriptorToSymbol, context.symbolTable, irFile)
        }

        irFile.acceptChildrenVoid(IrSyntheticDeclarationGenerator(context))

        insertImplicitCasts(irFile, context)
        context.callToSubstitutedDescriptorMap.clear()

        irFile.acceptVoid(AnnotationGenerator(context))

        irFile.patchDeclarationParents()

        return irFile
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

    fun <D> buildReceiverParameter(
        parent: D,
        origin: IrDeclarationOrigin,
        type: IrType,
        startOffset: Int = parent.startOffset,
        endOffset: Int = parent.endOffset
    ): IrValueParameter
            where D : IrDeclaration, D : IrDeclarationParent =
        parent.factory.createValueParameter(
            startOffset, endOffset, origin,
            IrValueParameterSymbolImpl(),
            Name.special("<this>"), -1, type, null, isCrossinline = false, isNoinline = false,
            isHidden = false, isAssignable = false
        ).also {
            it.parent = parent
        }


    fun generateEvaluatorModuleFragment(
        ktFile: KtBlockCodeFragment,
        fragmentClassDescriptor: ClassDescriptor,
        fragmentMethodDescriptor: FunctionDescriptor
    ): IrModuleFragment {
        return IrModuleFragmentImpl(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            irModule.files.add(
                createEmptyIrFile(ktFile).also { irFile ->
                    val klass = context.symbolTable.declareClass(fragmentClassDescriptor) {
                        context.irFactory.createIrClassFromDescriptor(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            IrDeclarationOrigin.DEFINED,
                            it,
                            fragmentClassDescriptor,
                            context.symbolTable.nameProvider.nameForDeclaration(fragmentClassDescriptor),
                            fragmentClassDescriptor.visibility,
                            fragmentClassDescriptor.modality
                        ).apply {
                            thisReceiver = buildReceiverParameter(
                                this,
                                IrDeclarationOrigin.INSTANCE_RECEIVER,
                                symbol.typeWithParameters(typeParameters)
                            )
                            val constructor = context.irFactory.createConstructor(
                                startOffset = UNDEFINED_OFFSET,
                                endOffset = UNDEFINED_OFFSET,
                                origin = IrDeclarationOrigin.DEFINED,
                                symbol = IrConstructorPublicSymbolImpl(context.symbolTable.signaturer.composeSignature(fragmentClassDescriptor)!!),
                                Name.special("<init>"),
                                fragmentClassDescriptor.visibility,
                                this.defaultType,
                                isInline = false,
                                isExternal = false,
                                isPrimary = true,
                                isExpect = false
                            )
                            constructor.parent = this
                            constructor.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
                            addMember(constructor)
                        }
                    }
                    context.symbolTable.declareSimpleFunction(fragmentMethodDescriptor) { functionSymbol ->
                        val function = context.irFactory.createFunction(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            IrDeclarationOrigin.DEFINED,
                            functionSymbol,
                            context.symbolTable.nameProvider.nameForDeclaration(fragmentMethodDescriptor),
                            fragmentMethodDescriptor.visibility,
                            fragmentMethodDescriptor.modality,
                            fragmentMethodDescriptor.returnType?.let { context.typeTranslator.translateType(it) }
                                ?: context.irBuiltIns.unitType,
                            isExpect = false,
                            isExternal = false,
                            isInfix = false,
                            isInline = false,
                            isOperator = false,
                            isSuspend = false,
                            isTailrec = false
                        ).apply {
                            parent = klass
                            body = BodyGenerator(
                                functionSymbol,
                                context
                            ).generateExpressionBody(ktFile.getContentElement())
                        }
                        klass.addMember(function)
                        function
                    }
                    irFile.declarations.add(klass)
                    irFile.patchDeclarationParents()
                }
            )
        }
    }
}
