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
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.OptionalParametersHelper
import org.jetbrains.kotlin.idea.core.resolveCandidates
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.containsError
import java.awt.Color
import java.util.*
import kotlin.reflect.KClass

class KotlinFunctionParameterInfoHandler : KotlinParameterInfoWithCallHandlerBase<KtValueArgumentList, KtValueArgument>(KtValueArgumentList::class, KtValueArgument::class) {
    override fun getActualParameters(arguments: KtValueArgumentList) = arguments.arguments.toTypedArray()

    override fun getActualParametersRBraceType() = KtTokens.RPAR

    override fun getArgumentListAllowedParentClasses() = setOf(KtCallElement::class.java)
}

class KotlinArrayAccessParameterInfoHandler : KotlinParameterInfoWithCallHandlerBase<KtContainerNode, KtExpression>(KtContainerNode::class, KtExpression::class) {
    override fun getArgumentListAllowedParentClasses() = setOf(KtArrayAccessExpression::class.java)

    override fun getActualParameters(containerNode: KtContainerNode): Array<out KtExpression> = containerNode.allChildren.filterIsInstance<KtExpression>().toList().toTypedArray()

    override fun getActualParametersRBraceType() = KtTokens.RBRACKET
}

abstract class KotlinParameterInfoWithCallHandlerBase<TArgumentList : KtElement, TArgument : KtElement>(
        private val argumentListClass: KClass<TArgumentList>,
        private val argumentClass: KClass<TArgument>
) : ParameterInfoHandlerWithTabActionSupport<TArgumentList, FunctionDescriptor, TArgument> {

    companion object {
        @JvmField
        val GREEN_BACKGROUND: Color = JBColor(Color(231, 254, 234), Gray._100)
    }

    private fun findCall(argumentList: TArgumentList, bindingContext: BindingContext): Call? {
        return (argumentList.parent as KtElement).getCall(bindingContext)
    }

    override fun getActualParameterDelimiterType() = KtTokens.COMMA

    override fun getArgListStopSearchClasses() = setOf(KtNamedFunction::class.java, KtVariableDeclaration::class.java)

    override fun getArgumentListClass() = argumentListClass.java

    override fun showParameterInfo(element: TArgumentList, context: CreateParameterInfoContext) {
        context.showHint(element, element.textRange.startOffset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): TArgumentList? {
        val element = context.file.findElementAt(context.offset) ?: return null
        val argumentList = PsiTreeUtil.getParentOfType(element, argumentListClass.java) ?: return null
        val argument = element.parents.takeWhile { it != argumentList }.lastOrNull()
        if (argument != null && !argumentClass.java.isInstance(argument)) {
            val arguments = getActualParameters(argumentList)
            val index = arguments.indexOf(element)
            context.setCurrentParameter(index)
            context.setHighlightedParameter(element)
        }
        return argumentList
    }

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): TArgumentList? {
        //todo: calls to this constructors, when we will have auxiliary constructors
        val file = context.file as? KtFile ?: return null

        val token = file.findElementAt(context.offset) ?: return null
        val argumentList = PsiTreeUtil.getParentOfType(token, argumentListClass.java) ?: return null

        val bindingContext = argumentList.analyze(BodyResolveMode.PARTIAL)
        val call = findCall(argumentList, bindingContext) ?: return null

        val candidates = call.resolveCandidates(bindingContext, file.getResolutionFacade())

        context.itemsToShow = candidates.map { it.resultingDescriptor.original }.distinct().toTypedArray()
        return argumentList
    }

    override fun updateParameterInfo(argumentList: TArgumentList, context: UpdateParameterInfoContext) {
        if (context.parameterOwner !== argumentList) {
            context.removeHint()
        }

        val offset = context.offset
        val parameterIndex = argumentList.allChildren
                .takeWhile { it.startOffset < offset }
                .count { it.node.elementType == KtTokens.COMMA }
        context.setCurrentParameter(parameterIndex)
    }

    override fun updateUI(itemToShow: FunctionDescriptor, context: ParameterInfoUIContext) {
        if (!updateUIOrFail(itemToShow, context)) {
            context.isUIComponentEnabled = false
            return
        }
    }

    private fun updateUIOrFail(itemToShow: FunctionDescriptor, context: ParameterInfoUIContext): Boolean {
        if (context.parameterOwner == null || !context.parameterOwner.isValid) return false
        if (!argumentListClass.java.isInstance(context.parameterOwner)) return false
        @Suppress("UNCHECKED_CAST")
        val argumentList = context.parameterOwner as TArgumentList

        val currentArgumentIndex = context.currentParameterIndex
        if (currentArgumentIndex < 0) return false // by some strange reason we are invoked with currentParameterIndex == -1 during initialization

        val bindingContext = argumentList.analyze(BodyResolveMode.PARTIAL)
        val call = findCall(argumentList, bindingContext) ?: return false

        val project = argumentList.project

        val (substitutedDescriptor, argumentToParameter, highlightParameterIndex, isGrey) = matchCallWithSignature(
                call, itemToShow, currentArgumentIndex, bindingContext, argumentList.getResolutionFacade()
        ) ?: return false

        var boldStartOffset = -1
        var boldEndOffset = -1
        val text = buildString {
            val usedParameterIndices = HashSet<Int>()
            var namedMode = false

            if (call.callType == Call.CallType.ARRAY_SET_METHOD) {
                // for set-operator the last parameter is used for the value assigned
                usedParameterIndices.add(substitutedDescriptor.valueParameters.lastIndex)
            }

            val includeParameterNames = !substitutedDescriptor.hasSynthesizedParameterNames()

            fun appendParameter(parameter: ValueParameterDescriptor) {
                if (length > 0) {
                    append(", ")
                }

                val highlightParameter = parameter.index == highlightParameterIndex
                if (highlightParameter) {
                    boldStartOffset = length
                }

                append(renderParameter(parameter, includeParameterNames, namedMode, project))

                if (highlightParameter) {
                    boldEndOffset = length
                }
            }

            for (argument in call.valueArguments) {
                if (argument is LambdaArgument) continue
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

            if (length == 0) {
                append(CodeInsightBundle.message("parameter.info.no.parameters"))
            }
        }


        val color = if (isResolvedToDescriptor(call, itemToShow, bindingContext))
            KotlinParameterInfoWithCallHandlerBase.GREEN_BACKGROUND
        else
            context.defaultParameterColor

        val isDeprecated = KotlinBuiltIns.isDeprecated(itemToShow)

        context.setupUIComponentPresentation(text, boldStartOffset, boldEndOffset, isGrey, isDeprecated, false, color)

        return true
    }

    override fun getParameterCloseChars() = ParameterInfoUtils.DEFAULT_PARAMETER_CLOSE_CHARS

    override fun tracksParameterIndex() = true

    //TODO
    override fun couldShowInLookup() = false
    override fun getParametersForLookup(item: LookupElement, context: ParameterInfoContext) = emptyArray<Any>()
    override fun getParametersForDocumentation(item: FunctionDescriptor, context: ParameterInfoContext) = emptyArray<Any>()

    private val RENDERER = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions {
        renderUnabbreviatedType = false
    }

    private fun renderParameter(parameter: ValueParameterDescriptor, includeName: Boolean, named: Boolean, project: Project): String {
        return buildString {
            if (named) append("[")

            if (parameter.varargElementType != null) {
                append("vararg ")
            }

            if (includeName) {
                append(parameter.name)
                append(": ")
            }

            append(RENDERER.renderType(parameterTypeToRender(parameter)))

            if (parameter.hasDefaultValue()) {
                append(" = ")
                append(parameter.renderDefaultValue(project))
            }

            if (named) append("]")
        }
    }

    private fun ValueParameterDescriptor.renderDefaultValue(project: Project): String {
        val expression = OptionalParametersHelper.defaultParameterValueExpression(this, project)
        if (expression != null) {
            val text = expression.text
            if (text.length <= 32) {
                return text
            }

            if (expression is KtConstantExpression || expression is KtStringTemplateExpression) {
                if (text.startsWith("\"")) {
                    return "\"...\""
                }
                else if (text.startsWith("\'")) {
                    return "\'...\'"
                }
            }
        }
        return "..."
    }

    private fun parameterTypeToRender(descriptor: ValueParameterDescriptor): KotlinType {
        var type = descriptor.varargElementType ?: descriptor.type
        if (type.containsError()) {
            val original = descriptor.original
            type = original.varargElementType ?: original.type
        }
        return type
    }

    private fun isResolvedToDescriptor(
            call: Call,
            functionDescriptor: FunctionDescriptor,
            bindingContext: BindingContext
    ): Boolean {
        val target = call.getResolvedCall(bindingContext)?.resultingDescriptor as? FunctionDescriptor
        return target != null && descriptorsEqual(target, functionDescriptor)
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

        val isArraySetMethod = call.callType == Call.CallType.ARRAY_SET_METHOD

        val arguments = call.valueArguments.let { args ->
            // For array set method call, we're only interested in the arguments in brackets which are all except the last one
            if (isArraySetMethod) args.dropLast(1) else args
        }

        assert(arguments.size >= currentArgumentIndex)

        val callToUse: Call
        val currentArgument: ValueArgument
        if (arguments.size > currentArgumentIndex) {
            currentArgument = arguments[currentArgumentIndex]
            callToUse = call
        }
        else {
            // add dummy current argument if we don't have one
            currentArgument = object : ValueArgument {
                override fun getArgumentExpression(): KtExpression? = null
                override fun getArgumentName(): ValueArgumentName? = null
                override fun isNamed(): Boolean = false
                override fun asElement(): KtElement = call.callElement // is a hack but what to do?
                override fun getSpreadElement(): LeafPsiElement? = null
                override fun isExternal() = false
            }
            callToUse = object : DelegatingCall(call) {
                val argumentsWithCurrent =
                        arguments + currentArgument +
                        // For array set method call, also add the argument in the right-hand side
                        (if (isArraySetMethod) listOf(call.valueArguments.last()) else listOf())

                override fun getValueArguments() = argumentsWithCurrent
                override fun getFunctionLiteralArguments() = emptyList<LambdaArgument>()
                override fun getValueArgumentList() = null
            }
        }

        val candidates = callToUse.resolveCandidates(bindingContext, resolutionFacade)
        // First try to find strictly matching descriptor, then one with the same declaration.
        // The second way is needed for the case when the descriptor was invalidated and new one has been built.
        // See testLocalFunctionBug().
        val resolvedCall = candidates.singleOrNull { it.resultingDescriptor.original == overload.original }
                           ?: candidates.singleOrNull { descriptorsEqual(it.resultingDescriptor, overload) }
                           ?: return null
        val resultingDescriptor = resolvedCall.resultingDescriptor

        fun argumentToParameter(argument: ValueArgument): ValueParameterDescriptor? {
            return (resolvedCall.getArgumentMapping(argument) as? ArgumentMatch)?.valueParameter
        }

        val highlightParameterIndex = argumentToParameter(currentArgument)?.index

        val argumentsBeforeCurrent = arguments.subList(0, currentArgumentIndex)
        if ((argumentsBeforeCurrent + currentArgument).any { argumentToParameter(it) == null }) {
            // some of arguments before the current one (or the current one) are not mapped to any of the parameters
            return SignatureInfo(resultingDescriptor, ::argumentToParameter, highlightParameterIndex, isGrey = true)
        }

        // grey out if not all arguments before the current are matched
        val isGrey = argumentsBeforeCurrent.any { argument ->
            resolvedCall.getArgumentMapping(argument).isError() &&
            !argument.hasError(bindingContext) /* ignore arguments that have error type */
        }
        return SignatureInfo(resultingDescriptor, ::argumentToParameter, highlightParameterIndex, isGrey)
    }

    private fun ValueArgument.hasError(bindingContext: BindingContext)
            = getArgumentExpression()?.let { bindingContext.getType(it) }?.isError ?: true

    // we should not compare descriptors directly because partial resolve is involved
    private fun descriptorsEqual(descriptor1: FunctionDescriptor, descriptor2: FunctionDescriptor): Boolean {
        if (descriptor1.original == descriptor2.original) return true
        val declaration1 = DescriptorToSourceUtils.descriptorToDeclaration(descriptor1) ?: return false
        val declaration2 = DescriptorToSourceUtils.descriptorToDeclaration(descriptor2)
        return declaration1 == declaration2
    }
}
