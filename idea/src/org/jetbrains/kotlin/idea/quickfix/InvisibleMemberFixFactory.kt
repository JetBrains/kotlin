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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

object InvisibleMemberFixFactory : KotlinIntentionActionsFactory() {

    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val element = diagnostic.psiElement as? KtElement ?: return emptyList()
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val usageModule = context.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, element.containingKtFile)?.module

        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return emptyList()
        val descriptor = resolvedCall.candidateDescriptor
        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtModifierListOwner ?: return emptyList()

        val module = DescriptorUtils.getContainingModule(descriptor)
        val targetVisibilities = when (descriptor.visibility) {
            Visibilities.PRIVATE, Visibilities.INVISIBLE_FAKE -> if (module == usageModule) listOf(Visibilities.PUBLIC, Visibilities.PROTECTED, Visibilities.INTERNAL) else listOf(Visibilities.PUBLIC, Visibilities.PROTECTED)
            else -> listOf(Visibilities.PUBLIC)
        }

        return targetVisibilities.mapNotNull { ChangeVisibilityFix.create(declaration, descriptor, it) }
    }

}