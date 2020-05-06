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

package org.jetbrains.kotlin.psi2ir

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
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
    val signaturer: IdSignatureComposer
) {
    private val postprocessingSteps = SmartList<Psi2IrPostprocessingStep>()

    fun addPostprocessingStep(step: Psi2IrPostprocessingStep) {
        postprocessingSteps.add(step)
    }

    // NOTE: used only for test purpose
    fun generateModule(
        moduleDescriptor: ModuleDescriptor,
        ktFiles: Collection<KtFile>,
        bindingContext: BindingContext,
        generatorExtensions: GeneratorExtensions,
        nameProvider: NameProvider = NameProvider.DEFAULT
    ): IrModuleFragment {
        val context = createGeneratorContext(moduleDescriptor, bindingContext, nameProvider, extensions = generatorExtensions)
        val irProviders = generateTypicalIrProviderList(
            moduleDescriptor, context.irBuiltIns, context.symbolTable, extensions = generatorExtensions
        )
        return generateModuleFragment(context, ktFiles, irProviders)
    }

    fun createGeneratorContext(
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext,
        nameProvider: NameProvider = NameProvider.DEFAULT,
        symbolTable: SymbolTable = SymbolTable(signaturer, nameProvider),
        extensions: GeneratorExtensions = GeneratorExtensions()
    ): GeneratorContext =
        createGeneratorContext(
            configuration, moduleDescriptor, bindingContext, languageVersionSettings, symbolTable, extensions
        )

    fun generateModuleFragment(
        context: GeneratorContext,
        ktFiles: Collection<KtFile>,
        irProviders: List<IrProvider>,
        expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null
    ): IrModuleFragment {
        val moduleGenerator = ModuleGenerator(context)
        val irModule = moduleGenerator.generateModuleFragmentWithoutDependencies(ktFiles)

        irModule.patchDeclarationParents()
        expectDescriptorToSymbol?.let { referenceExpectsForUsedActuals(it, context.symbolTable, irModule) }
        postprocess(context, irModule)

        irProviders.filterIsInstance<IrDeserializer>().forEach { it.init(irModule) }

        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)

        assert(context.symbolTable.allUnbound.isEmpty())
        postprocessingSteps.forEach { it.invoke(irModule) }
//        assert(context.symbolTable.allUnbound.isEmpty()) // TODO: fix IrPluginContext to make it not produce additional external reference

        // TODO: remove it once plugin API improved
        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)

        return irModule
    }

    private fun postprocess(context: GeneratorContext, irElement: IrModuleFragment) {
        generateSyntheticDeclarations(irElement, context)
        insertImplicitCasts(irElement, context)
        generateAnnotationsForDeclarations(context, irElement)

        irElement.patchDeclarationParents()
    }

    private fun generateAnnotationsForDeclarations(context: GeneratorContext, irElement: IrElement) {
        val annotationGenerator = AnnotationGenerator(context)
        irElement.acceptVoid(annotationGenerator)
    }

    private fun generateSyntheticDeclarations(moduleFragment: IrModuleFragment, context: GeneratorContext) {
        val generator = IrSyntheticDeclarationGenerator(context)
        moduleFragment.acceptChildrenVoid(generator)
    }
}
