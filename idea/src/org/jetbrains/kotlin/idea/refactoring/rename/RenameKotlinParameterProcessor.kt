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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeSignatureConfiguration
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.modify
import org.jetbrains.kotlin.idea.refactoring.changeSignature.runChangeSignature
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.OverrideResolver

public class RenameKotlinParameterProcessor : RenameKotlinPsiProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is KtParameter && element.getParent().getParent() is KtNamedFunction
    }

    override fun renameElement(element: PsiElement, newName: String, usages: Array<out UsageInfo>, listener: RefactoringElementListener?) {
        val function = (element as KtParameter).getParent().getParent() as KtNamedFunction
        val paramIndex = function.getValueParameters().indexOf(element)
        assert(paramIndex != -1, { "couldn't find parameter in parent ${element.getElementTextWithContext()}" })

        val functionDescriptor = function.resolveToDescriptor() as? FunctionDescriptor ?: return
        val parameterDescriptor = functionDescriptor.getValueParameters()[paramIndex]

        val parameterNameChangedOnOverride = parameterDescriptor.getOverriddenDescriptors().any {
            overriddenParameter -> OverrideResolver.shouldReportParameterNameOverrideWarning(parameterDescriptor, overriddenParameter)
        }

        val changeSignatureConfiguration = object : KotlinChangeSignatureConfiguration {
            override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                val paramInfoIndex = if (functionDescriptor.getExtensionReceiverParameter() != null) paramIndex + 1 else paramIndex
                return originalDescriptor.modify { it.renameParameter(paramInfoIndex, newName) }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>) = true

            override fun forcePerformForSelectedFunctionOnly() = parameterNameChangedOnOverride
        }

        runChangeSignature(element.getProject(), functionDescriptor, changeSignatureConfiguration, element, "Rename parameter")
    }
}
