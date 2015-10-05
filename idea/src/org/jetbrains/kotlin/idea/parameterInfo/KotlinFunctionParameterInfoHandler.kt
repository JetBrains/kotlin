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

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.typeUtil.containsError
import java.awt.Color
import java.util.*

class KotlinFunctionParameterInfoHandler : ParameterInfoHandlerWithTabActionSupport<JetValueArgumentList, FunctionDescriptor, JetValueArgument> {

    override fun getActualParameters(arguments: JetValueArgumentList) = arguments.arguments.toTypedArray()

    override fun getActualParameterDelimiterType() = JetTokens.COMMA

    override fun getActualParametersRBraceType() = JetTokens.RBRACE

    override fun getArgumentListAllowedParentClasses() = setOf(JetCallElement::class.java)

    override fun getArgListStopSearchClasses() = setOf(JetFunction::class.java)

    override fun getArgumentListClass() = JetValueArgumentList::class.java

    override fun couldShowInLookup() = true

    override fun getParametersForLookup(item: LookupElement, context: ParameterInfoContext) = emptyArray<Any>() //todo: ?

    override fun getParametersForDocumentation(item: FunctionDescriptor, context: ParameterInfoContext) = emptyArray<Any>() //todo: ?

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): JetValueArgumentList? {
        //todo: calls to this constructors, when we will have auxiliary constructors
        val file = context.file as? JetFile ?: return null

        val argumentList = file.findElementAt(context.offset)?.getStrictParentOfType<JetValueArgumentList>() ?: return null

        val callElement = argumentList.parent as? JetCallElement ?: return null
        val bindingContext = callElement.analyze(BodyResolveMode.PARTIAL)
        val call = callElement.getCall(bindingContext) ?: return null

        val candidates = detectCandidates(call, bindingContext, file.getResolutionFacade())

        context.itemsToShow = candidates.map { it.resultingDescriptor.original }.toTypedArray()
        return argumentList
    }

    override fun showParameterInfo(element: JetValueArgumentList, context: CreateParameterInfoContext) {
        context.showHint(element, element.textRange.startOffset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): JetValueArgumentList? {
        return findCallAndUpdateContext(context)
    }

    override fun updateParameterInfo(argumentList: JetValueArgumentList, context: UpdateParameterInfoContext) {
        if (context.parameterOwner !== argumentList) {
            context.removeHint()
        }

        val offset = context.offset
        val parameterIndex = argumentList.allChildren
                .takeWhile { it.startOffset < offset }
                .count { it.node.elementType == JetTokens.COMMA }
        context.setCurrentParameter(parameterIndex)
    }

    override fun getParameterCloseChars() = ParameterInfoUtils.DEFAULT_PARAMETER_CLOSE_CHARS

    override fun tracksParameterIndex() = true

    override fun updateUI(itemToShow: FunctionDescriptor, context: ParameterInfoUIContext) {
        if (!updateUIOrFail(itemToShow, context)) {
            context.isUIComponentEnabled = false
            return
        }
    }

    private fun updateUIOrFail(itemToShow: FunctionDescriptor, context: ParameterInfoUIContext): Boolean {
        //todo: when we will have ability to pass Array as vararg, implement such feature here too?
        if (context.parameterOwner == null || !context.parameterOwner.isValid) return false

        val argumentList = context.parameterOwner as? JetValueArgumentList ?: return false

        val bindingContext = argumentList.analyze(BodyResolveMode.PARTIAL)
        val callElement = argumentList.parent as? JetCallElement ?: return false
        val call = callElement.getCall(bindingContext) ?: return false

        val currentParameterIndex = context.currentParameterIndex
        if (currentParameterIndex < 0) return false // by some strange reason we are invoked with currentParameterIndex == -1 during initialization

        val (substitutedDescriptor, argumentToParameter, highlightParameterIndex, isGrey) = matchCallWithSignature(
                call, itemToShow, currentParameterIndex, bindingContext, argumentList.getResolutionFacade()
        ) ?: return false

        var boldStartOffset = -1
        var boldEndOffset = -1
        val text = StringBuilder {
            val usedParameterIndices = HashSet<Int>()
            var namedMode = false

            fun appendParameter(parameter: ValueParameterDescriptor) {
                if (length() > 0) {
                    append(", ")
                }

                val highlightParameter = parameter.index == highlightParameterIndex
                if (highlightParameter) {
                    boldStartOffset = length()
                }

                append(renderParameter(parameter, namedMode))

                if (highlightParameter) {
                    boldEndOffset = length()
                }
            }

            for (argument in argumentList.arguments) {
                val parameter = argumentToParameter(argument) ?: continue
                if (!usedParameterIndices.add(parameter.index)) continue

                if (argument.isNamed()) {
                    namedMode = true
                }

                appendParameter(parameter)
            }

            for (parameter in substitutedDescriptor.valueParameters) {
                if (parameter.index !in usedParameterIndices) {
                    appendParameter(parameter)
                }
            }

            if (length() == 0) {
                append(CodeInsightBundle.message("parameter.info.no.parameters"))
            }
        }.toString()


        val color = if (isResolvedToDescriptor(argumentList, itemToShow, bindingContext))
            GREEN_BACKGROUND
        else
            context.defaultParameterColor

        val isDeprecated = KotlinBuiltIns.isDeprecated(itemToShow)

        context.setupUIComponentPresentation(text, boldStartOffset, boldEndOffset, isGrey, isDeprecated, false, color)

        return true
    }

    companion object {
        val GREEN_BACKGROUND: Color = JBColor(Color(231, 254, 234), Gray._100)

        private fun renderParameter(parameter: ValueParameterDescriptor, named: Boolean): String {
            return StringBuilder {
                if (named) append("[")
                if (parameter.varargElementType != null) {
                    append("vararg ")
                }
                append(parameter.name)
                append(": ")
                append(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(getActualParameterType(parameter)))
                if (parameter.hasDefaultValue()) {
                    val parameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(parameter)
                    append(" = ")
                    append(getDefaultExpressionString(parameterDeclaration))
                }
                if (named) append("]")
            }.toString()
        }

        private fun getDefaultExpressionString(parameterDeclaration: PsiElement?): String {
            if (parameterDeclaration is JetParameter) {
                val defaultValue = parameterDeclaration.defaultValue
                if (defaultValue != null) {
                    val defaultExpression = defaultValue.text
                    if (defaultExpression.length() <= 32) {
                        return defaultExpression
                    }

                    if (defaultValue is JetConstantExpression || defaultValue is JetStringTemplateExpression) {
                        if (defaultExpression.startsWith("\"")) {
                            return "\"...\""
                        }
                        else if (defaultExpression.startsWith("\'")) {
                            return "\'...\'"
                        }
                    }
                }
            }
            return "..."
        }

        private fun getActualParameterType(descriptor: ValueParameterDescriptor): JetType {
            var type = descriptor.varargElementType ?: descriptor.type
            if (type.containsError()) {
                val original = descriptor.original
                type = original.varargElementType ?: original.type
            }
            return type
        }

        private fun isResolvedToDescriptor(
                argumentList: JetValueArgumentList,
                functionDescriptor: FunctionDescriptor,
                bindingContext: BindingContext
        ): Boolean {
            val callNameExpression = getCallNameExpression(argumentList)
            if (callNameExpression != null) {
                val declarationDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, callNameExpression]
                if (declarationDescriptor?.original === functionDescriptor.original) return true
            }

            return false
        }

        private fun getCallNameExpression(argumentList: JetValueArgumentList): JetSimpleNameExpression? {
            return (argumentList.parent as? JetCallElement)?.getCallNameExpression()
        }

        private fun findCallAndUpdateContext(context: UpdateParameterInfoContext): JetValueArgumentList? {
            var element = context.file.findElementAt(context.offset) ?: return null
            var parent = element.parent
            while (parent != null && parent !is JetValueArgumentList) {
                element = element!!.parent
                parent = parent.parent
            }
            if (parent == null) return null

            val argumentList = parent as JetValueArgumentList
            if (element is JetValueArgument) {
                val i = argumentList.arguments.indexOf(element)
                context.setCurrentParameter(i)
                context.setHighlightedParameter(element)
            }
            return argumentList
        }

        private data class SignatureInfo(
                val substitutedDescriptor: FunctionDescriptor,
                val argumentToParameter: (ValueArgument) -> ValueParameterDescriptor?,
                val highlightParameterIndex: Int?,
                val isGrey: Boolean
        )

        private fun matchCallWithSignature(
                call: Call,
                overload: FunctionDescriptor,
                currentArgumentIndex: Int,
                bindingContext: BindingContext,
                resolutionFacade: ResolutionFacade
        ): SignatureInfo? {
            if (currentArgumentIndex == 0 && call.valueArguments.isEmpty() && overload.valueParameters.isEmpty()) {
                return SignatureInfo(overload, { null }, null, isGrey = false)
            }

            assert(call.valueArguments.size() >= currentArgumentIndex)

            val argumentsBeforeCurrent = call.valueArguments.subList(0, currentArgumentIndex)

            val callToUse: Call
            val currentArgument: ValueArgument
            if (call.valueArguments.size() > currentArgumentIndex) {
                currentArgument = call.valueArguments[currentArgumentIndex]
                callToUse = call
            }
            else {
                // add dummy current argument if we don't have one
                currentArgument = object : ValueArgument {
                    override fun getArgumentExpression(): JetExpression? = null
                    override fun getArgumentName(): ValueArgumentName? = null
                    override fun isNamed(): Boolean = false
                    override fun asElement(): JetElement = call.callElement // is a hack but what to do?
                    override fun getSpreadElement(): LeafPsiElement? = null
                    override fun isExternal() = false
                }
                callToUse = object : DelegatingCall(call) {
                    val arguments = call.valueArguments + currentArgument

                    override fun getValueArguments() = arguments
                    override fun getFunctionLiteralArguments() = emptyList<FunctionLiteralArgument>()
                    override fun getValueArgumentList() = null
                }
            }

            val candidates = detectCandidates(callToUse, bindingContext, resolutionFacade)
            val resolvedCall = candidates.singleOrNull { it.resultingDescriptor.original == overload.original } ?: return null
            val resultingDescriptor = resolvedCall.resultingDescriptor

            val argumentToParameter = { argument: ValueArgument -> (resolvedCall.getArgumentMapping(argument) as? ArgumentMatch)?.valueParameter }

            val currentParameter = (resolvedCall.getArgumentMapping(currentArgument) as? ArgumentMatch)?.valueParameter
            val highlightParameterIndex = currentParameter?.index

            if (!(argumentsBeforeCurrent + currentArgument).all { argument -> resolvedCall.getArgumentMapping(argument) is ArgumentMatch }) { // some of arguments before the current one are not mapped to any of the parameters
                return SignatureInfo(resultingDescriptor, argumentToParameter, highlightParameterIndex, isGrey = true)
            }

            // grey out if not all arguments before the current are matched
            val isGrey = argumentsBeforeCurrent
                    .any { argument -> resolvedCall.getArgumentMapping(argument).isError() && !argument.hasError(bindingContext) /* ignore arguments that has error type */ }
            return SignatureInfo(resultingDescriptor, argumentToParameter, highlightParameterIndex, isGrey)
        }

        private fun ValueArgument.hasError(bindingContext: BindingContext)
                = getArgumentExpression()?.let { bindingContext.getType(it) }?.isError ?: true

        private fun detectCandidates(call: Call, bindingContext: BindingContext, resolutionFacade: ResolutionFacade): List<ResolvedCall<FunctionDescriptor>> {
            val callElement = call.callElement
            val resolutionScope = callElement.getResolutionScope(bindingContext, resolutionFacade)
            val inDescriptor = resolutionScope.ownerDescriptor

            val dataFlowInfo = bindingContext.getDataFlowInfo(call.calleeExpression)
            val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace")
            val expectedType = (callElement as? JetExpression)?.let {
                bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, it.getQualifiedExpressionForSelectorOrThis()]
            } ?: TypeUtils.NO_EXPECTED_TYPE
            val callResolutionContext = BasicCallResolutionContext.create(
                    bindingTrace, resolutionScope, call, expectedType, dataFlowInfo,
                    ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    CallChecker.DoNothing, false/*TODO?*/
            ).replaceCollectAllCandidates(true)
            val callResolver = resolutionFacade.frontendService<CallResolver>()

            val results: OverloadResolutionResults<FunctionDescriptor> = callResolver.resolveFunctionCall(callResolutionContext)

            return results.allCandidates!!
                    .filter { it.status != ResolutionStatus.RECEIVER_TYPE_ERROR && it.status != ResolutionStatus.RECEIVER_PRESENCE_ERROR }
                    .filter {
                        val thisReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(it.dispatchReceiver, bindingContext)
                        Visibilities.isVisible(thisReceiver, it.resultingDescriptor, inDescriptor)
                    }
        }
    }
}
