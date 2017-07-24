/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.js

import org.jetbrains.kotlin.backend.js.context.IrTranslationConfig
import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.backend.js.declarations.IrDeclarationTranslationVisitor
import org.jetbrains.kotlin.backend.js.lower.JavaScriptLower
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.BindingTrace

class IrBasedTranslator(private val config: JsConfig, private val bindingTrace: BindingTrace, private val module: ModuleDescriptor) {
    fun translate(files: Collection<KtFile>, scope: JsScope): Collection<JsProgramFragment> {
        val psi2ir = Psi2IrTranslator()
        val psi2irContext = psi2ir.createGeneratorContext(module, bindingTrace.bindingContext)
        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files)

        val translationConfig = IrTranslationConfig(irModuleFragment, bindingTrace, config, scope)
        val fragments = mutableListOf<JsProgramFragment>()
        for (file in irModuleFragment.files) {
            val fragment = JsProgramFragment(scope)
            val context = IrTranslationContext(translationConfig, fragment)
            translateFile(context, file)
            fragments += fragment
        }
        return fragments
    }

    private fun translateFile(context: IrTranslationContext, file: IrFile) {
        JavaScriptLower(context.module).lower(file)

        val visitor = IrDeclarationTranslationVisitor(context)
        for (declaration in file.declarations) {
            declaration.acceptVoid(visitor)
        }
    }
}