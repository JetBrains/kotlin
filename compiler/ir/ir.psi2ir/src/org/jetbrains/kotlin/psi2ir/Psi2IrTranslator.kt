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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.ModuleGenerator
import org.jetbrains.kotlin.psi2ir.transformations.insertImplicitCasts
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.SmartList

class Psi2IrTranslator(val configuration: Psi2IrConfiguration = Psi2IrConfiguration()) {
    interface PostprocessingStep {
        fun postprocess(context: GeneratorContext, irElement: IrElement)
    }

    private val postprocessingSteps = SmartList<PostprocessingStep>()

    fun add(step: PostprocessingStep) {
        postprocessingSteps.add(step)
    }

    fun generateModule(moduleDescriptor: ModuleDescriptor, ktFiles: Collection<KtFile>, bindingContext: BindingContext): IrModuleFragment {
        val context = createGeneratorContext(moduleDescriptor, bindingContext)
        return generateModuleFragment(context, ktFiles)
    }

    fun createGeneratorContext(moduleDescriptor: ModuleDescriptor, bindingContext: BindingContext) =
        GeneratorContext(configuration, moduleDescriptor, bindingContext)

    fun generateModuleFragment(context: GeneratorContext, ktFiles: Collection<KtFile>): IrModuleFragment {
        val irModule = ModuleGenerator(context).generateModuleFragment(ktFiles)
        postprocess(context, irModule)
        return irModule
    }

    private fun postprocess(context: GeneratorContext, irElement: IrElement) {
        insertImplicitCasts(context.builtIns, irElement)

        postprocessingSteps.forEach { it.postprocess(context, irElement) }
    }
}
