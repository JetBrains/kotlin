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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.AnnotationGenerator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.psi2ir.generators.ModuleGenerator
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
        val context = createGeneratorContext(moduleDescriptor, bindingContext, extensions = generatorExtensions)
        val irProviders = generateTypicalIrProviderList(
            moduleDescriptor, context.irBuiltIns, context.symbolTable, extensions = generatorExtensions
        )
        return generateModuleFragment(context, ktFiles, irProviders)
    }

    fun createGeneratorContext(
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext,
        symbolTable: SymbolTable = SymbolTable(mangler),
        extensions: GeneratorExtensions = GeneratorExtensions()
    ): GeneratorContext =
        GeneratorContext(configuration, moduleDescriptor, bindingContext, languageVersionSettings, symbolTable, extensions)

    fun generateModuleFragment(
        context: GeneratorContext,
        ktFiles: Collection<KtFile>,
        irProviders: List<IrProvider>
    ): IrModuleFragment {
        val moduleGenerator = ModuleGenerator(context)
        val irModule = moduleGenerator.generateModuleFragmentWithoutDependencies(ktFiles)

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
