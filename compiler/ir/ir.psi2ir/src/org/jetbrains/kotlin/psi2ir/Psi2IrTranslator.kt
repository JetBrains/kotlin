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
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.psi2ir.generators.ModuleGenerator
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.psi2ir.generators.fragments.EvaluatorFragmentInfo
import org.jetbrains.kotlin.psi2ir.generators.fragments.FragmentContext
import org.jetbrains.kotlin.psi2ir.generators.fragments.FragmentModuleGenerator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.SmartList

fun interface Psi2IrPostprocessingStep {
    fun invoke(irModuleFragment: IrModuleFragment)
}

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
        extensions: GeneratorExtensions = GeneratorExtensions(),
        fragmentContext: FragmentContext? = null
    ): GeneratorContext {
        val typeTranslator = TypeTranslatorImpl(symbolTable, languageVersionSettings, moduleDescriptor, extensions = extensions)
        return GeneratorContext(
            configuration,
            moduleDescriptor,
            bindingContext,
            languageVersionSettings,
            symbolTable,
            extensions,
            typeTranslator,
            IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable),
            fragmentContext
        )
    }

    fun generateModuleFragment(
        context: GeneratorContext,
        ktFiles: Collection<KtFile>,
        irProviders: List<IrProvider>,
        linkerExtensions: Collection<IrDeserializer.IrLinkerExtension>,
        expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null,
        fragmentInfo: EvaluatorFragmentInfo? = null
    ): IrModuleFragment {
        val moduleGenerator = fragmentInfo?.let {
            FragmentModuleGenerator(context, it)
        } ?: ModuleGenerator(context, expectDescriptorToSymbol)

        val irModule = moduleGenerator.generateModuleFragment(ktFiles)

        val deserializers = irProviders.filterIsInstance<IrDeserializer>()
        deserializers.forEach { it.init(irModule, linkerExtensions) }

        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)

        deserializers.forEach { it.postProcess() }
        context.symbolTable.noUnboundLeft("Unbound symbols not allowed\n")

        postprocessingSteps.forEach { it.invoke(irModule) }
//        assert(context.symbolTable.allUnbound.isEmpty()) // TODO: fix IrPluginContext to make it not produce additional external reference

        // TODO: remove it once plugin API improved
        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)
        deserializers.forEach { it.postProcess() }

        return irModule
    }
}
