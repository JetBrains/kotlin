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
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.IrProvider
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
    val configuration: Psi2IrConfiguration,
) {
    private val postprocessingSteps = SmartList<Psi2IrPostprocessingStep>()

    fun addPostprocessingStep(step: Psi2IrPostprocessingStep) {
        postprocessingSteps.add(step)
    }

    fun createGeneratorContext(
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext,
        symbolTable: SymbolTable,
        extensions: GeneratorExtensions = GeneratorExtensions()
    ): GeneratorContext {
        val typeTranslator = TypeTranslator(symbolTable, languageVersionSettings, moduleDescriptor.builtIns, extensions = extensions)
        val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)
        typeTranslator.constantValueGenerator = constantValueGenerator
        constantValueGenerator.typeTranslator = typeTranslator
        return GeneratorContext(
            configuration,
            moduleDescriptor,
            bindingContext,
            languageVersionSettings,
            symbolTable,
            extensions,
            typeTranslator,
            constantValueGenerator,
            IrBuiltIns(moduleDescriptor.builtIns, typeTranslator, symbolTable),
        )
    }

    fun generateModuleFragment(
        context: GeneratorContext,
        ktFiles: Collection<KtFile>,
        irProviders: List<IrProvider>,
        linkerExtensions: Collection<IrDeserializer.IrLinkerExtension>,
        expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null
    ): IrModuleFragment {
        val moduleGenerator = ModuleGenerator(context)
        val irModule = moduleGenerator.generateModuleFragmentWithoutDependencies(ktFiles)

        irModule.patchDeclarationParents()
        expectDescriptorToSymbol?.let { referenceExpectsForUsedActuals(it, context.symbolTable, irModule) }
        postprocess(context, irModule)

        val deserializers = irProviders.filterIsInstance<IrDeserializer>()
        deserializers.forEach { it.init(irModule, linkerExtensions) }

        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)

        deserializers.forEach { it.postProcess() }
        val allUnbound = context.symbolTable.allUnbound
        assert(allUnbound.isEmpty()) { "Unbound symbols not allowed\n${allUnbound.joinToString("\n\t", "\t")}" }

        postprocessingSteps.forEach { it.invoke(irModule) }
//        assert(context.symbolTable.allUnbound.isEmpty()) // TODO: fix IrPluginContext to make it not produce additional external reference

        // TODO: remove it once plugin API improved
        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)
        deserializers.forEach { it.postProcess() }

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
