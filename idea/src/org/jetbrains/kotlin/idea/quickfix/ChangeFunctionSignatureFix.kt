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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.mapArgumentsToParameters
import org.jetbrains.kotlin.idea.util.getDataFlowAwareTypes
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeSignatureConfiguration
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.modify
import org.jetbrains.kotlin.idea.refactoring.changeSignature.runChangeSignature
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

abstract class ChangeFunctionSignatureFix(
    element: PsiElement,
    protected val functionDescriptor: FunctionDescriptor
) : KotlinQuickFixAction<PsiElement>(element) {
    override fun getFamilyName() = FAMILY_NAME

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val declarations = DescriptorToSourceUtilsIde.getAllDeclarations(project, functionDescriptor)
        return declarations.isNotEmpty() && declarations.all { it.isValid && it.canRefactor() }
    }

    protected fun getNewArgumentName(argument: ValueArgument, validator: Function1<String, Boolean>): String {
        val argumentName = argument.getArgumentName()
        val expression = argument.getArgumentExpression()

        return when {
            argumentName != null -> KotlinNameSuggester.suggestNameByName(argumentName.asName.asString(), validator)
            expression != null -> {
                val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)
                if (expression.text == "it") {
                    val type = expression.getType(bindingContext)
                    if (type != null) {
                        return KotlinNameSuggester.suggestNamesByType(type, validator, "param").first()
                    }
                }
                KotlinNameSuggester.suggestNamesByExpressionAndType(expression, null, bindingContext, validator, "param").first()
            }
            else -> KotlinNameSuggester.suggestNameByName("param", validator)
        }
    }

    companion object : KotlinSingleIntentionActionFactoryWithDelegate<KtCallElement, CallableDescriptor>() {
        override fun getElementOfInterest(diagnostic: Diagnostic): KtCallElement? = diagnostic.psiElement.getStrictParentOfType()

        override fun extractFixData(element: KtCallElement, diagnostic: Diagnostic): CallableDescriptor? {
            return DiagnosticFactory.cast(diagnostic, Errors.TOO_MANY_ARGUMENTS, Errors.NO_VALUE_FOR_PARAMETER).a
        }

        override fun createFix(originalElement: KtCallElement, data: CallableDescriptor): ChangeFunctionSignatureFix? {
            val functionDescriptor = data as? FunctionDescriptor
                ?: (data as? ValueParameterDescriptor)?.containingDeclaration as? FunctionDescriptor
                ?: return null

            if (functionDescriptor.kind == SYNTHESIZED) return null

            if (data is ValueParameterDescriptor) {
                return RemoveParameterFix(originalElement, functionDescriptor, data)
            } else {
                val parameters = functionDescriptor.valueParameters
                val arguments = originalElement.valueArguments

                if (arguments.size > parameters.size) {
                    val bindingContext = originalElement.analyze()
                    val call = originalElement.getCall(bindingContext) ?: return null
                    val argumentToParameter = call.mapArgumentsToParameters(functionDescriptor)
                    val hasTypeMismatches = argumentToParameter.any { (argument, parameter) ->
                        val argumentTypes = argument.getArgumentExpression()?.let {
                            getDataFlowAwareTypes(
                                it,
                                bindingContext
                            )
                        }
                        argumentTypes?.none { dataFlowAwareType ->
                            KotlinTypeChecker.DEFAULT.isSubtypeOf(dataFlowAwareType, parameter.type)
                        } ?: true
                    }
                    return AddFunctionParametersFix(originalElement, functionDescriptor, hasTypeMismatches)
                }
            }

            return null
        }

        private class RemoveParameterFix(
            element: PsiElement,
            functionDescriptor: FunctionDescriptor,
            private val parameterToRemove: ValueParameterDescriptor
        ) : ChangeFunctionSignatureFix(element, functionDescriptor) {

            override fun getText() = "Remove parameter '${parameterToRemove.name.asString()}'"

            override fun invoke(project: Project, editor: Editor?, file: KtFile) {
                runRemoveParameter(parameterToRemove, element ?: return)
            }
        }

        const val FAMILY_NAME = "Change signature of function/constructor"

        fun runRemoveParameter(parameterDescriptor: ValueParameterDescriptor, context: PsiElement) {
            val functionDescriptor = parameterDescriptor.containingDeclaration as FunctionDescriptor
            runChangeSignature(
                context.project,
                functionDescriptor,
                object : KotlinChangeSignatureConfiguration {
                    override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                        return originalDescriptor.modify { descriptor ->
                            val index = if (descriptor.receiver != null) parameterDescriptor.index + 1 else parameterDescriptor.index
                            descriptor.removeParameter(index)
                        }
                    }

                    override fun performSilently(affectedFunctions: Collection<PsiElement>) = true
                    override fun forcePerformForSelectedFunctionOnly() = false
                },
                context,
                "Remove parameter '${parameterDescriptor.name.asString()}'"
            )
        }

    }
}


