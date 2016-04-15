/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ExposedVisibilityChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.idea.core.setVisibility

class IncreaseVisibilityFix(
        element: KtModifierListOwner,
        private val elementName: String,
        private val visibilityModifier: KtModifierKeywordToken
) : KotlinQuickFixAction<KtModifierListOwner>(element), CleanupFix {

    override fun getText() = "Make $elementName $visibilityModifier"
    override fun getFamilyName() = "Make $visibilityModifier"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element.setVisibility(visibilityModifier)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement as? KtElement ?: return null
            val context = element.analyze(BodyResolveMode.PARTIAL)
            val usageModule = context.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, element.getContainingKtFile())?.module

            @Suppress("UNCHECKED_CAST")
            val factory = diagnostic.factory as DiagnosticFactory3<*, DeclarationDescriptor, *, DeclarationDescriptor>
            val descriptor = factory.cast(diagnostic).c as? DeclarationDescriptorWithVisibility ?: return null
            val declaration = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor) as? KtModifierListOwner ?: return null

            val module = DescriptorUtils.getContainingModule(descriptor)
            val (modifier, visibility) = if (module != usageModule || descriptor.visibility != Visibilities.PRIVATE) {
                Pair(KtTokens.PUBLIC_KEYWORD, Visibilities.PUBLIC)
            }
            else {
                Pair(KtTokens.INTERNAL_KEYWORD, Visibilities.INTERNAL)
            }

            if (!ExposedVisibilityChecker().checkDeclarationWithVisibility(declaration, descriptor, visibility)) return null

            return IncreaseVisibilityFix(declaration, descriptor.name.asString(), modifier)
        }
    }
}
