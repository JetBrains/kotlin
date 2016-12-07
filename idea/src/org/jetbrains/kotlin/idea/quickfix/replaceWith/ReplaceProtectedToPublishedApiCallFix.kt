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

package org.jetbrains.kotlin.idea.quickfix.replaceWith

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi

class ReplaceProtectedToPublishedApiCallFix(
        element: KtExpression,
        val classOwnerPointer: SmartPsiElementPointer<KtClass>,
        val originalName: String,
        val paramNames: Map<String, String>,
        val signature: String
) : KotlinQuickFixAction<KtExpression>(element)  {

    override fun getFamilyName() = "Replace with @PublishedApi bridge call"

    val newFunctionName = "`${originalName.newName}`"

    override fun getText() = "Replace with generated @PublishedApi bridge call '$newFunctionName(...)'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val isPublishedFunctionAlreadyExists = false/*TODO*/
        if (!isPublishedFunctionAlreadyExists) {
            val classOwner = classOwnerPointer.element ?: return

            val function = KtPsiFactory(classOwner).createFunction(
                    "@kotlin.PublishedApi\n" +
                    "internal "+//${extensionType?.let { it + "." } ?: ""}$newFunctionName(${paramNames.entries.map { it.key + " :" + it.value }.joinToString(", ")}) = " +
                    signature.replaceFirst("$originalName(", "$newFunctionName(") +
                    " = $originalName(${paramNames.keys.map { it }.joinToString(", ")})"
            )
            val newFunction = classOwner.addDeclaration(function)
            ShortenReferences.DEFAULT.process(newFunction)
        }
        element.replace(KtPsiFactory(element).createExpression(newFunctionName))
    }

    companion object : KotlinSingleIntentionActionFactory() {
        val signatureRenderer = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
            renderDefaultValues = false
            startFromDeclarationKeyword = true
            withoutReturnType = true
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val psiElement = diagnostic.psiElement as? KtExpression ?: return null
            val descriptor = DiagnosticFactory.cast(diagnostic, Errors.PROTECTED_CALL_FROM_PUBLIC_INLINE).a
            val isProperty = descriptor is PropertyAccessorDescriptor || descriptor is PropertyDescriptor
            if (isProperty) return null/*TODO support properties*/

            val signature = signatureRenderer.render(descriptor)
            val paramNameAndType = descriptor.valueParameters.associate { it.name.asString() to it.type.getJetTypeFqName(false)}
            val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return null
            val source = classDescriptor.source.getPsi() as? KtClass ?: return null
            return ReplaceProtectedToPublishedApiCallFix(psiElement, source.createSmartPointer(), descriptor.name.asString(), paramNameAndType, signature)
        }

        val String.newName: String
            get() = "access\$$this"
    }
}
