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
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.types.KotlinType

class ChangeFunctionLiteralSignatureFix private constructor(
        functionLiteral: KtFunctionLiteral,
        functionDescriptor: FunctionDescriptor,
        private val parameterTypes: List<KotlinType>
) : ChangeFunctionSignatureFix(functionLiteral, functionDescriptor) {

    override fun getText() = "Change the signature of lambda expression"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        runChangeSignature(
                project,
                functionDescriptor,
                object : KotlinChangeSignatureConfiguration {
                    override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                        return originalDescriptor.modify { descriptor ->
                            val validator = CollectingNameValidator()
                            descriptor.clearNonReceiverParameters()
                            for (type in parameterTypes) {
                                val name = KotlinNameSuggester.suggestNamesByType(type, validator, "param")[0]
                                descriptor.addParameter(KotlinParameterInfo(functionDescriptor, -1, name, KotlinTypeInfo(false, type)))
                            }
                        }
                    }

                    override fun performSilently(affectedFunctions: Collection<PsiElement>) = false
                    override fun forcePerformForSelectedFunctionOnly() = false
                },
                element,
                text)
    }

    companion object : KotlinSingleIntentionActionFactoryWithDelegate<KtFunctionLiteral, Companion.Data>() {
        data class Data(val descriptor: FunctionDescriptor, val parameterTypes: List<KotlinType>)

        override fun getElementOfInterest(diagnostic: Diagnostic): KtFunctionLiteral? {
            val diagnosticWithParameters = Errors.EXPECTED_PARAMETERS_NUMBER_MISMATCH.cast(diagnostic)
            return diagnosticWithParameters.psiElement as? KtFunctionLiteral
        }

        override fun extractFixData(element: KtFunctionLiteral, diagnostic: Diagnostic): Data? {
            val descriptor = element.resolveToDescriptorIfAny() as? FunctionDescriptor ?: return null
            val parameterTypes = Errors.EXPECTED_PARAMETERS_NUMBER_MISMATCH.cast(diagnostic).b
            return Data(descriptor, parameterTypes)
        }

        override fun createFix(originalElement: KtFunctionLiteral, data: Data): IntentionAction?
                = ChangeFunctionLiteralSignatureFix(originalElement, data.descriptor, data.parameterTypes)
    }
}
