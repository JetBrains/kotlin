/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.factories.DefaultIrDeclarationFactory
import org.jetbrains.kotlin.ir.factories.IrDeclarationFactory
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.*
import org.jetbrains.kotlin.psi2ir.transformations.insertImplicitCasts
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.SmartList

typealias Psi2IrPostprocessingStep = (IrModuleFragment) -> Unit

class Psi2IrTranslator(
    val languageVersionSettings: LanguageVersionSettings,
    val configuration: Psi2IrConfiguration = Psi2IrConfiguration(),
    val mangler: KotlinMangler? = null
) {
    private val postprocessingSteps = SmartList<Psi2IrPostprocessingStep>()

    fun addPostprocessingStep(step: Psi2IrPostprocessingStep) {
        postprocessingSteps.add(step)
    }

    fun generateModule(
        moduleDescriptor: ModuleDescriptor,
        ktFiles: Collection<KtFile>,
        bindingContext: BindingContext,
        generatorExtensions: GeneratorExtensions
    ): IrModuleFragment {
        val context = createGeneratorContext(
            moduleDescriptor, bindingContext,
            extensions = generatorExtensions,
            irDeclarationFactory = DefaultIrDeclarationFactory() ///
        )
        val irProviders = generateTypicalIrProviderList(
            moduleDescriptor, context.irBuiltIns, context.symbolTable, extensions = generatorExtensions
        )
        return generateModuleFragment(context, ktFiles, irProviders)
    }

    fun createGeneratorContext(
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext,
        irDeclarationFactory: IrDeclarationFactory,
        symbolTable: SymbolTable = SymbolTable(irDeclarationFactory, mangler),
        extensions: GeneratorExtensions = GeneratorExtensions()
    ): GeneratorContext =
        GeneratorContext(configuration, moduleDescriptor, bindingContext,
                         languageVersionSettings, symbolTable, irDeclarationFactory, extensions)

    fun generateModuleFragment(
        context: GeneratorContext,
        ktFiles: Collection<KtFile>,
        irProviders: List<IrProvider>,
        expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null
    ): IrModuleFragment {
        val moduleGenerator = ModuleGenerator(context)
        val irModule = moduleGenerator.generateModuleFragmentWithoutDependencies(ktFiles)

        expectDescriptorToSymbol ?. let { referenceExpectsForUsedActuals(it, context.symbolTable, irModule) }
        irModule.patchDeclarationParents()
        postprocess(context, irModule)
        // do not generate unbound symbols before postprocessing,
        // since plugins must work with non-lazy IR
        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)
        irModule.computeUniqIdForDeclarations(context.symbolTable)

        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)

        return irModule
    }

    private fun postprocess(context: GeneratorContext, irElement: IrModuleFragment) {
        insertImplicitCasts(irElement, context)
        generateAnnotationsForDeclarations(context, irElement)

        postprocessingSteps.forEach { it(irElement) }

        irElement.patchDeclarationParents()
    }

    private fun generateAnnotationsForDeclarations(context: GeneratorContext, irElement: IrElement) {
        val annotationGenerator = AnnotationGenerator(context)
        irElement.acceptVoid(annotationGenerator)
    }
}
