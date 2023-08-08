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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
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
    private val checkNoUnboundSymbols: (SymbolTable, String) -> Unit
) {
    @Deprecated("Only for backward compatibility with older versions of IDE", level = DeprecationLevel.ERROR)
    constructor(
        languageVersionSettings: LanguageVersionSettings,
        configuration: Psi2IrConfiguration
    ) : this(languageVersionSettings, configuration, checkNoUnboundSymbols = { _, _ -> })

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
        val typeTranslator = TypeTranslatorImpl(
            symbolTable, languageVersionSettings, moduleDescriptor, extensions = extensions,
            allowErrorTypeInAnnotations = configuration.skipBodies,
        )
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
        fragmentInfo: EvaluatorFragmentInfo? = null
    ): IrModuleFragment {

        val moduleGenerator = fragmentInfo?.let {
            FragmentModuleGenerator(context, it)
        } ?: ModuleGenerator(context)

        val irModule = moduleGenerator.generateModuleFragment(ktFiles)

        val deserializers = irProviders.filterIsInstance<IrDeserializer>()
        deserializers.forEach { it.init(irModule, linkerExtensions) }

        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)

        deserializers.forEach { it.postProcess(inOrAfterLinkageStep = true) }
        context.checkNoUnboundSymbols { "after generation of IR module ${irModule.name.asString()}" }

        postprocessingSteps.forEach { it.invoke(irModule) }
//        assert(context.symbolTable.allUnbound.isEmpty()) // TODO: fix IrPluginContext to make it not produce additional external reference

        // TODO: remove it once plugin API improved
        moduleGenerator.generateUnboundSymbolsAsDependencies(irProviders)
        deserializers.forEach { it.postProcess(inOrAfterLinkageStep = true) }
        context.checkNoUnboundSymbols { "after applying all post-processing steps for the generated IR module ${irModule.name.asString()}" }

        return irModule
    }

    private fun GeneratorContext.checkNoUnboundSymbols(whenDetected: () -> String) {
        if (!configuration.partialLinkageEnabled)
            checkNoUnboundSymbols(symbolTable, whenDetected())
    }
}
