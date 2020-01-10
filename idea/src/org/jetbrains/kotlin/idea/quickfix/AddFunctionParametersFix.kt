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
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.util.getDataFlowAwareTypes
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.*

class AddFunctionParametersFix(
    callElement: KtCallElement,
    functionDescriptor: FunctionDescriptor,
    private val hasTypeMismatches: Boolean,
    private val argumentIndex: Int? = null
) : ChangeFunctionSignatureFix(callElement, functionDescriptor) {
    private val callElement: KtCallElement?
        get() = element as? KtCallElement

    private val typesToShorten = ArrayList<KotlinType>()

    override fun getText(): String {
        val callElement = callElement ?: return ""

        val parameters = functionDescriptor.valueParameters
        val arguments = callElement.valueArguments
        val newParametersCnt = arguments.size - parameters.size
        assert(newParametersCnt > 0)

        val subjectSuffix = if (newParametersCnt > 1) "s" else ""

        val callableDescription = if (isConstructor()) {
            val className = functionDescriptor.containingDeclaration.name.asString()
            "constructor '$className'"
        } else {
            val functionName = functionDescriptor.name.asString()
            "function '$functionName'"
        }

        val parameterOrdinal = argumentIndex?.let {
            val number = (it + 1).toString()
            val suffix = when {
                number.endsWith("1") -> "st"
                number.endsWith("2") -> "nd"
                number.endsWith("3") -> "rd"
                else -> "th"
            }
            "$number$suffix "
        } ?: ""

        return if (hasTypeMismatches)
            "Change the signature of $callableDescription"
        else
            "Add ${parameterOrdinal}parameter$subjectSuffix to $callableDescription"
    }

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false
        val callElement = callElement ?: return false

        // newParametersCnt <= 0: psi for this quickfix is no longer valid
        val newParametersCnt = callElement.valueArguments.size - functionDescriptor.valueParameters.size
        if (argumentIndex != null && newParametersCnt != 1) return false
        return newParametersCnt > 0
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val callElement = callElement ?: return
        runChangeSignature(project, functionDescriptor, addParameterConfiguration(), callElement, text)
    }

    private fun addParameterConfiguration(): KotlinChangeSignatureConfiguration {
        return object : KotlinChangeSignatureConfiguration {
            override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                return originalDescriptor.modify(fun(descriptor: KotlinMutableMethodDescriptor) {
                    val callElement = callElement ?: return
                    val arguments = callElement.valueArguments
                    val parameters = functionDescriptor.valueParameters
                    val validator = CollectingNameValidator()

                    if (argumentIndex != null) {
                        parameters.forEach { validator.addName(it.name.asString()) }
                        val argument = arguments[argumentIndex]
                        val parameterInfo = getNewParameterInfo(
                            originalDescriptor.baseDescriptor as FunctionDescriptor,
                            argument,
                            validator
                        )
                        descriptor.addParameter(argumentIndex, parameterInfo)
                        return
                    }

                    val call = callElement.getCall(callElement.analyze()) ?: return
                    for (i in arguments.indices) {
                        val argument = arguments[i]
                        val expression = argument.getArgumentExpression()

                        if (i < parameters.size) {
                            validator.addName(parameters[i].name.asString())
                            val argumentType = expression?.let {
                                val bindingContext = it.analyze()
                                val smartCasts = bindingContext[BindingContext.SMARTCAST, it]
                                smartCasts?.defaultType ?: smartCasts?.type(call) ?: bindingContext.getType(it)
                            }
                            val parameterType = parameters[i].type

                            if (argumentType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(argumentType, parameterType)) {
                                descriptor.parameters[i].currentTypeInfo = KotlinTypeInfo(false, argumentType)
                                typesToShorten.add(argumentType)
                            }
                        } else {
                            val parameterInfo = getNewParameterInfo(
                                originalDescriptor.baseDescriptor as FunctionDescriptor,
                                argument,
                                validator
                            )
                            descriptor.addParameter(parameterInfo)
                        }
                    }
                })
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean {
                val onlyFunction = affectedFunctions.singleOrNull() ?: return false
                return !hasTypeMismatches && !isConstructor() && !hasOtherUsages(onlyFunction)
            }
        }
    }

    private fun getNewParameterInfo(
        functionDescriptor: FunctionDescriptor,
        argument: ValueArgument,
        validator: (String) -> Boolean
    ): KotlinParameterInfo {
        val name = getNewArgumentName(argument, validator)
        val expression = argument.getArgumentExpression()
        val type = expression?.let { getDataFlowAwareTypes(it).firstOrNull() } ?: functionDescriptor.builtIns.nullableAnyType
        return KotlinParameterInfo(functionDescriptor, -1, name, KotlinTypeInfo(false, null)).apply {
            currentTypeInfo = KotlinTypeInfo(false, type)
            originalTypeInfo.type?.let { typesToShorten.add(it) }
            if (expression != null) defaultValueForCall = expression
        }
    }

    private fun hasOtherUsages(function: PsiElement): Boolean {
        return ReferencesSearch.search(function).any {
            val call = it.element.getParentOfType<KtCallElement>(false)
            call != null && callElement != call
        }
    }

    private fun isConstructor() = functionDescriptor is ConstructorDescriptor

    companion object TypeMismatchFactory : KotlinSingleIntentionActionFactoryWithDelegate<KtCallElement, Pair<FunctionDescriptor, Int>>() {
        override fun getElementOfInterest(diagnostic: Diagnostic): KtCallElement? {
            val (_, valueArgumentList) = diagnostic.valueArgument() ?: return null
            return valueArgumentList.getStrictParentOfType()
        }

        override fun extractFixData(element: KtCallElement, diagnostic: Diagnostic): Pair<FunctionDescriptor, Int>? {
            val (valueArgument, valueArgumentList) = diagnostic.valueArgument() ?: return null
            val arguments = valueArgumentList.arguments
            val argumentIndex = arguments.indexOfFirst { it == valueArgument }
            val context = element.analyze()
            val functionDescriptor = element.getResolvedCall(context)?.resultingDescriptor as? FunctionDescriptor ?: return null
            val parameters = functionDescriptor.valueParameters
            if (arguments.size - 1 != parameters.size) return null
            if ((arguments - valueArgument).zip(parameters).any { (argument, parameter) ->
                    val argumentType = argument.getArgumentExpression()?.let { context.getType(it) }
                    argumentType == null || !KotlinTypeChecker.DEFAULT.isSubtypeOf(argumentType, parameter.type)
                }) return null

            return functionDescriptor to argumentIndex
        }

        override fun createFix(originalElement: KtCallElement, data: Pair<FunctionDescriptor, Int>): IntentionAction? {
            val (functionDescriptor, argumentIndex) = data
            val parameters = functionDescriptor.valueParameters
            val arguments = originalElement.valueArguments
            return if (arguments.size > parameters.size) {
                AddFunctionParametersFix(originalElement, functionDescriptor, false, argumentIndex)
            } else {
                null
            }
        }

        private fun Diagnostic.valueArgument(): Pair<KtValueArgument, KtValueArgumentList>? {
            val element = DiagnosticFactory.cast(this, Errors.TYPE_MISMATCH).psiElement
            val valueArgument = element.getStrictParentOfType<KtValueArgument>() ?: return null
            val valueArgumentList = valueArgument.getStrictParentOfType<KtValueArgumentList>() ?: return null
            return valueArgument to valueArgumentList
        }
    }
}
