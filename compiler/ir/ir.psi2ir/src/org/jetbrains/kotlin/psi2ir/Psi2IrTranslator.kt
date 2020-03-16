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
        generatorExtensions: GeneratorExtensions
    ): IrModuleFragment {
        val context = createGeneratorContext(moduleDescriptor, bindingContext, extensions = generatorExtensions)
        val irProviders = generateTypicalIrProviderList(
            moduleDescriptor, context.irBuiltIns, context.symbolTable, extensions = generatorExtensions
        )
        return generateModuleFragment(context, ktFiles, irProviders)
    }

    fun createGeneratorContext(
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext,
        symbolTable: SymbolTable = SymbolTable(signaturer),
        extensions: GeneratorExtensions = GeneratorExtensions()
    ): GeneratorContext =
        createGeneratorContext(configuration, moduleDescriptor, bindingContext, languageVersionSettings, symbolTable, extensions, signaturer)

    fun generateModuleFragment(
        context: GeneratorContext,
        ktFiles: Collection<KtFile>,
        irProviders: List<IrProvider>,
        expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null,
        pluginExtensions: Collection<IrExtensionGenerator> = emptyList()
    ): IrModuleFragment {
        val moduleGenerator = ModuleGenerator(context)
        val irModule = moduleGenerator.generateModuleFragmentWithoutDependencies(ktFiles)

        irModule.patchDeclarationParents()
        expectDescriptorToSymbol?.let { referenceExpectsForUsedActuals(it, context.symbolTable, irModule) }
        postprocess(context, irModule)

        irProviders.filterIsInstance<IrDeserializer>().forEach { it.init(irModule, pluginExtensions) }

        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)

        postprocessingSteps.forEach { it.invoke(irModule) }

        // TODO: remove it once plugin API improved
        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)

        return irModule
    }

    private fun postprocess(context: GeneratorContext, irElement: IrModuleFragment) {
        insertImplicitCasts(irElement, context)
        generateAnnotationsForDeclarations(context, irElement)

        irElement.patchDeclarationParents()
    }

    private fun generateAnnotationsForDeclarations(context: GeneratorContext, irElement: IrElement) {
        val annotationGenerator = AnnotationGenerator(context)
        irElement.acceptVoid(annotationGenerator)
    }
}
