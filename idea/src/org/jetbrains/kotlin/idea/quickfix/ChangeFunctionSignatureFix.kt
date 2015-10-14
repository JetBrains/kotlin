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
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.EXPECTED_PARAMETERS_NUMBER_MISMATCH
import org.jetbrains.kotlin.diagnostics.Errors.UNUSED_PARAMETER
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.checker.JetTypeChecker

abstract class ChangeFunctionSignatureFix(
        protected val context: PsiElement,
        protected val functionDescriptor: FunctionDescriptor
) : KotlinQuickFixAction<PsiElement>(context) {

    override fun getFamilyName() = "Change signature of function/constructor"

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false

        val declarations = DescriptorToSourceUtilsIde.getAllDeclarations(project, functionDescriptor)
        return declarations.all { it.isValid && QuickFixUtil.canModifyElement(it) }
    }

    protected fun getNewArgumentName(argument: ValueArgument, validator: Function1<String, Boolean>): String {
        val argumentName = argument.getArgumentName()
        val expression = argument.getArgumentExpression()

        if (argumentName != null) {
            return KotlinNameSuggester.suggestNameByName(argumentName.asName.asString(), validator)
        }
        else if (expression != null) {
            val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)
            return KotlinNameSuggester.suggestNamesByExpressionAndType(expression, bindingContext, validator, "param").first()
        }
        else {
            return KotlinNameSuggester.suggestNameByName("param", validator)
        }
    }

    object Factory : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): ChangeFunctionSignatureFix? {
            val callElement = PsiTreeUtil.getParentOfType(diagnostic.psiElement, JetCallElement::class.java) ?: return null
            val descriptor = DiagnosticFactory.cast(diagnostic, Errors.TOO_MANY_ARGUMENTS, Errors.NO_VALUE_FOR_PARAMETER).a
            return createFix(callElement, callElement, descriptor)
        }
    }

    object FactoryForParametersNumberMismatch: JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): ChangeFunctionSignatureFix? {
            val diagnosticWithParameters = EXPECTED_PARAMETERS_NUMBER_MISMATCH.cast(diagnostic)
            val functionLiteral = diagnosticWithParameters.psiElement as? JetFunctionLiteral ?: return null
            val descriptor = functionLiteral.resolveToDescriptor() as? FunctionDescriptor ?: return null
            return ChangeFunctionLiteralSignatureFix(functionLiteral, descriptor, diagnosticWithParameters.b)
        }
    }

    object FactoryForUnusedParameter : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): ChangeFunctionSignatureFix? {
            val descriptor = UNUSED_PARAMETER.cast(diagnostic).a as? ValueParameterDescriptor ?: return null
            return createFix(null, diagnostic.psiElement, descriptor)
        }
    }
}

private fun createFix(callElement: JetCallElement?, context: PsiElement, descriptor: CallableDescriptor): ChangeFunctionSignatureFix? {
    val functionDescriptor = when (descriptor) {
        is FunctionDescriptor -> descriptor as FunctionDescriptor
        else -> if (descriptor is ValueParameterDescriptor) descriptor.containingDeclaration as? FunctionDescriptor
        else null
    } ?: return null

    if (functionDescriptor.kind == SYNTHESIZED) return null

    if (descriptor is ValueParameterDescriptor) {
        return RemoveFunctionParametersFix(context, functionDescriptor, descriptor)
    }
    else {
        val parameters = functionDescriptor.valueParameters
        val arguments = callElement!!.valueArguments

        if (arguments.size > parameters.size) {
            val hasTypeMismatches = hasTypeMismatches(parameters, arguments, callElement.analyze())
            return AddFunctionParametersFix(callElement, functionDescriptor, hasTypeMismatches)
        }
    }

    return null
}

private fun hasTypeMismatches(
        parameters: List<ValueParameterDescriptor>,
        arguments: List<ValueArgument>,
        bindingContext: BindingContext
): Boolean {
    assert(parameters.size <= arguments.size) // number of parameters must not be greater than the number of arguments (it's called only for TOO_MANY_ARGUMENTS error)
    for ((parameter, argument) in parameters.zip(arguments)) {
        val argumentType = argument.getArgumentExpression()?.let { bindingContext.getType(it) }
        if (argumentType == null || !JetTypeChecker.DEFAULT.isSubtypeOf(argumentType, parameter.type)) return true
    }
    return false
}

