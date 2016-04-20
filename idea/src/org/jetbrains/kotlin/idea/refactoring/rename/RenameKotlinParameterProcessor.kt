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
import com.intellij.util.xml.impl.GenericDomValueReference
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeSignatureConfiguration
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.modify
import org.jetbrains.kotlin.idea.refactoring.changeSignature.runChangeSignature
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.OverrideResolver

class RenameKotlinParameterProcessor : RenameKotlinPsiProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is KtParameter && element.parent.parent is KtNamedFunction
    }

    override fun renameElement(element: PsiElement, newName: String, usages: Array<out UsageInfo>, listener: RefactoringElementListener?) {
        // Workaround for usages in XML files
        // TODO: Do not use Change Signature for parameter rename as it's less efficient and loses some usages
        for (usage in usages) {
            (usage.reference as? GenericDomValueReference<*>)?.handleElementRename(newName)
        }

        val function = (element as KtParameter).parent.parent as KtNamedFunction
        val paramIndex = function.valueParameters.indexOf(element)
        assert(paramIndex != -1, { "couldn't find parameter in parent ${element.getElementTextWithContext()}" })

        val functionDescriptor = function.resolveToDescriptor() as? FunctionDescriptor ?: return
        val parameterDescriptor = functionDescriptor.valueParameters[paramIndex]

        val parameterNameChangedOnOverride = parameterDescriptor.overriddenDescriptors.any {
            overriddenParameter -> OverrideResolver.shouldReportParameterNameOverrideWarning(parameterDescriptor, overriddenParameter)
        }

        val changeSignatureConfiguration = object : KotlinChangeSignatureConfiguration {
            override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                val paramInfoIndex = if (functionDescriptor.extensionReceiverParameter != null) paramIndex + 1 else paramIndex
                return originalDescriptor.modify { it.renameParameter(paramInfoIndex, newName) }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>) = true

            override fun forcePerformForSelectedFunctionOnly() = parameterNameChangedOnOverride
        }

        runChangeSignature(element.project, functionDescriptor, changeSignatureConfiguration, element, "Rename parameter")
    }
}
