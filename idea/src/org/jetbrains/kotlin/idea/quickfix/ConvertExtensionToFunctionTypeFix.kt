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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class ConvertExtensionToFunctionTypeFix(element: KtTypeReference, type: KotlinType) : KotlinQuickFixAction<KtTypeReference>(element) {

    private val targetTypeStringShort = type.renderType(IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES)
    private val targetTypeStringLong = type.renderType(IdeDescriptorRenderers.SOURCE_CODE)

    override fun getText() = "Convert supertype to '$targetTypeStringShort'"
    override fun getFamilyName() = "Convert extension function type to regular function type"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val replaced = element.replaced(KtPsiFactory(project).createType(targetTypeStringLong))
        ShortenReferences.DEFAULT.process(replaced)
    }

    private fun KotlinType.renderType(renderer: DescriptorRenderer) = buildString {
        append('(')
        arguments.dropLast(1).map { renderer.renderType(it.type) }.joinTo(this@buildString, ", ")
        append(") -> ")
        append(renderer.renderType(this@renderType.getReturnTypeFromFunctionType()))
    }

    companion object Factory : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val casted = Errors.SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE.cast(diagnostic)
            val element = casted.psiElement

            val type = element.analyze(BodyResolveMode.PARTIAL).get(BindingContext.TYPE, element) ?: return emptyList()
            if (!type.isExtensionFunctionType) return emptyList()

            return listOf(ConvertExtensionToFunctionTypeFix(element, type))
        }
    }
}
